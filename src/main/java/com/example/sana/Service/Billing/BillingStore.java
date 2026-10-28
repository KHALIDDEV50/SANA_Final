package com.example.sana.Service.Billing;

import com.example.sana.Config.BillingProperties;
import com.example.sana.DTO.Billing.BillingDTOs.*;
import com.example.sana.Enums.BillingPlan;
import com.example.sana.Model.*;
import com.example.sana.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BillingStore {
    private final UserRepository users;
    private final ProjectRepository projects;
    private final BillingAccountRepository accounts;
    private final BillingOrderRepository orders;
    private final BillingProperties properties;

    @Transactional(readOnly = true)
    public Membership membership(Integer userId) {
        BillingAccount account = accounts.findById(userId).orElse(null);
        boolean active = active(account);
        BillingPlan plan = active ? account.getPlanCode() : BillingPlan.SPARK;
        return new Membership(plan, active || account == null || account.getPlanCode() == BillingPlan.SPARK ? "ACTIVE" : "EXPIRED",
                projects.countByOwner_Id(userId), plan.maxProjects(), account == null ? null : account.getCurrentPeriodEnd(),
                properties.isEnabled(), properties.isTestMode(), "MANUAL");
    }

    // The calling project transaction retains this user lock through the project insert.
    @Transactional
    public void checkProjectQuota(Integer userId) {
        lockUser(userId);
        if (!properties.isEnabled()) return;
        Membership membership = membership(userId);
        if (membership.maxProjects() != null && membership.projectsUsed() >= membership.maxProjects())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project limit reached. Upgrade your plan to create another project");
    }

    @Transactional
    public BillingOrder reserve(Integer userId, BillingPlan plan) {
        User user = lockUser(userId);
        if (plan == BillingPlan.SPARK) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The Spark plan does not require payment");
        if (active(accounts.findById(userId).orElse(null)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A paid plan is already active. Renew after its current period ends");
        for (BillingOrder order : orders.findTop10ByUser_IdOrderByCreatedAtDesc(userId)) {
            if (("CREATING".equals(order.getStatus()) || "PENDING".equals(order.getStatus()))
                    && order.getExpiresAt().isAfter(Instant.now())) {
                if ("PENDING".equals(order.getStatus()) && order.getPlanCode() == plan) return order;
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A checkout is already pending. Complete it or wait for its expiry");
            }
        }
        BillingOrder order = new BillingOrder();
        order.setId(UUID.randomUUID().toString()); order.setUser(user); order.setPlanCode(plan);
        order.setAmountHalalas(plan.amountHalalas()); order.setCreatedAt(Instant.now());
        order.setExpiresAt(Instant.now().plusSeconds(1800));
        byte[] token = new byte[32]; new SecureRandom().nextBytes(token);
        order.setCallbackToken(HexFormat.of().formatHex(token));
        return orders.saveAndFlush(order);
    }

    @Transactional
    public void attachInvoice(String orderId, BillingGateway.Invoice invoice) {
        lockUser(orderOwner(orderId));
        BillingOrder order = get(orderId);
        validateInvoice(order, invoice, false);
        order.setInvoiceId(invoice.id()); order.setCheckoutUrl(invoice.checkoutUrl()); order.setStatus("PENDING");
        orders.saveAndFlush(order);
    }

    @Transactional
    public void failedCreation(String orderId) {
        lockUser(orderOwner(orderId));
        BillingOrder order = get(orderId);
        if ("CREATING".equals(order.getStatus())) { order.setStatus("CREATE_FAILED"); orders.save(order); }
    }

    @Transactional(readOnly = true)
    public BillingOrder callbackOrder(String orderId, String token) {
        BillingOrder order = get(orderId);
        if (!MessageDigest.isEqual(order.getCallbackToken().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid payment notification");
        if (order.getInvoiceId() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is not ready");
        return order;
    }

    @Transactional(readOnly = true)
    public List<BillingOrder> recentOrders(Integer userId) { return orders.findTop10ByUser_IdOrderByCreatedAtDesc(userId); }

    @Transactional
    public void applyVerifiedInvoice(String orderId, BillingGateway.Invoice invoice) {
        User user = lockUser(orderOwner(orderId));
        BillingOrder order = get(orderId);
        validateInvoice(order, invoice, true);
        if ("paid".equals(invoice.status())) {
            // Duplicate callbacks and repeated reconciliation must never extend a subscription twice.
            if ("PAID".equals(order.getStatus()) || "REVERSED".equals(order.getStatus())) return;
            BillingAccount account = accounts.findById(user.getId()).orElseGet(() -> {
                BillingAccount created = new BillingAccount(); created.setUser(user); return created;
            });
            Instant start = Instant.now();
            // An unexpected second paid invoice requires manual reconciliation, never a silent downgrade.
            if (active(account)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Paid invoice needs billing review: another subscription is active");
            account.setPlanCode(order.getPlanCode());
            account.setCurrentPeriodEnd(start.atZone(ZoneOffset.UTC).plusMonths(1).toInstant());
            account.setActiveOrderId(order.getId()); accounts.save(account);
            order.setStatus("PAID"); order.setPaidAt(start); orders.saveAndFlush(order);
        } else if (Set.of("refunded", "voided", "canceled").contains(invoice.status()) && "PAID".equals(order.getStatus())) {
            BillingAccount account = accounts.findById(user.getId()).orElse(null);
            if (account != null && order.getId().equals(account.getActiveOrderId())) {
                account.setPlanCode(BillingPlan.SPARK); account.setCurrentPeriodEnd(null); account.setActiveOrderId(null); accounts.save(account);
            }
            order.setStatus("REVERSED"); orders.save(order);
        } else if (!"PAID".equals(order.getStatus()) && !"REVERSED".equals(order.getStatus())
                && Set.of("expired", "canceled", "voided", "failed").contains(invoice.status())) {
            order.setStatus(invoice.status().toUpperCase(Locale.ROOT)); orders.save(order);
        }
    }

    private boolean active(BillingAccount account) {
        return account != null && account.getPlanCode() != BillingPlan.SPARK
                && account.getCurrentPeriodEnd() != null && account.getCurrentPeriodEnd().isAfter(Instant.now());
    }
    private void validateInvoice(BillingOrder order, BillingGateway.Invoice invoice, boolean checkId) {
        if ((checkId && !invoice.id().equals(order.getInvoiceId())) || invoice.amountHalalas() != order.getAmountHalalas()
                || !order.getCurrency().equals(invoice.currency()))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invoice verification failed");
    }
    private User lockUser(Integer id) {
        return users.findBillingUserForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
    // Read a scalar before the lock so JPA cannot cache stale order state while waiting.
    private Integer orderOwner(String id) {
        return orders.findOwnerId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found"));
    }
    private BillingOrder get(String id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found"));
    }
}
