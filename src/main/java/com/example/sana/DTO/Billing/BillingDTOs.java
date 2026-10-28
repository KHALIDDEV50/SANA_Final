package com.example.sana.DTO.Billing;

import com.example.sana.Enums.BillingPlan;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class BillingDTOs {
    private BillingDTOs() { }
    public record CheckoutRequest(@NotNull BillingPlan planCode) { }
    public record CheckoutResponse(String orderId, String checkoutUrl, boolean testMode) { }
    public record Membership(BillingPlan planCode, String status, long projectsUsed, Integer maxProjects,
                             Instant currentPeriodEnd, boolean paymentsEnabled, boolean testMode,
                             String renewalMode) { }
    public record Plan(BillingPlan code, int amountHalalas, String currency, Integer maxProjects) { }
    public record Order(String id, BillingPlan planCode, int amountHalalas, String currency,
                        String status, Instant createdAt, Instant paidAt) { }
    public record Catalog(List<Plan> plans, boolean paymentsEnabled, boolean testMode, String renewalMode) { }
}
