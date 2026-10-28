package com.example.sana.Service.Billing;

import com.example.sana.Config.BillingProperties;
import com.example.sana.DTO.Billing.BillingDTOs.*;
import com.example.sana.Enums.BillingPlan;
import com.example.sana.Model.BillingOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BillingService {
    private final BillingStore store;
    private final BillingGateway gateway;
    private final BillingProperties properties;

    public Catalog catalog() {
        return new Catalog(Arrays.stream(BillingPlan.values()).map(p -> new Plan(p, p.amountHalalas(), "SAR", p.maxProjects())).toList(),
                properties.isEnabled(), properties.isTestMode(), "MANUAL");
    }
    public Membership membership(Integer userId) { return store.membership(userId); }
    public List<Order> history(Integer userId) {
        return store.recentOrders(userId).stream().map(o -> new Order(o.getId(), o.getPlanCode(), o.getAmountHalalas(),
                o.getCurrency(), o.getStatus(), o.getCreatedAt(), o.getPaidAt())).toList();
    }
    public CheckoutResponse checkout(Integer userId, BillingPlan plan) {
        enabled();
        BillingOrder order = store.reserve(userId, plan);
        if (order.getInvoiceId() != null) return new CheckoutResponse(order.getId(), order.getCheckoutUrl(), properties.isTestMode());
        // Provider HTTP calls run outside database transactions and locks.
        try {
            String callback = properties.publicOrigin() + "/api/billing/callbacks/" + order.getId() + "/" + order.getCallbackToken();
            BillingGateway.Invoice invoice = gateway.createInvoice(order, callback,
                    properties.frontendOrigin() + "/#/billing-result/pending",
                    properties.frontendOrigin() + "/#/billing-result/cancelled");
            store.attachInvoice(order.getId(), invoice);
            return new CheckoutResponse(order.getId(), invoice.checkoutUrl(), properties.isTestMode());
        } catch (RuntimeException e) { store.failedCreation(order.getId()); throw e; }
    }
    public void callback(String orderId, String token) {
        enabled();
        BillingOrder order = store.callbackOrder(orderId, token);
        // Ignore the callback body. Fetch the authoritative invoice with the private server key.
        store.applyVerifiedInvoice(orderId, gateway.fetchInvoice(order.getInvoiceId()));
    }
    public Membership reconcile(Integer userId) {
        enabled();
        for (BillingOrder order : store.recentOrders(userId)) {
            if (order.getInvoiceId() != null && ("PENDING".equals(order.getStatus()) || "PAID".equals(order.getStatus())))
                store.applyVerifiedInvoice(order.getId(), gateway.fetchInvoice(order.getInvoiceId()));
        }
        return membership(userId);
    }
    private void enabled() {
        if (!properties.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Billing is disabled");
    }
}
