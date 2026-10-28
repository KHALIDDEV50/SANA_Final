package com.example.sana.Controller;

import com.example.sana.API.ApiResponse;
import com.example.sana.DTO.Billing.BillingDTOs.Catalog;
import com.example.sana.DTO.Billing.BillingDTOs.CheckoutRequest;
import com.example.sana.DTO.Billing.BillingDTOs.CheckoutResponse;
import com.example.sana.DTO.Billing.BillingDTOs.Membership;
import com.example.sana.DTO.Billing.BillingDTOs.Order;
import com.example.sana.Model.User;
import com.example.sana.Service.Billing.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billing;

    // Get the available plans and payment configuration.
    @GetMapping("/plans")
    public Catalog plans() {
        return billing.catalog();
    }

    // Get the authenticated user's membership.
    @GetMapping("/me")
    public Membership me(@AuthenticationPrincipal User user) {
        return billing.membership(user.getId());
    }

    // Get the authenticated user's payment orders.
    // Order refers to BillingDTOs.Order, not the JPA criteria type.
    @GetMapping("/orders")
    public List<Order> orders(@AuthenticationPrincipal User user) {
        return billing.history(user.getId());
    }

    // Create a checkout using the authenticated user's account.
    // The backend determines the amount and currency.
    @PostMapping("/checkout")
    public CheckoutResponse checkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CheckoutRequest request) {

        return billing.checkout(user.getId(), request.planCode());
    }

    // Verify payment status directly with Moyasar.
    @PostMapping("/reconcile")
    public Membership reconcile(@AuthenticationPrincipal User user) {
        return billing.reconcile(user.getId());
    }

    // Only this notification route is public.
    // The service verifies the token and retrieves the invoice from Moyasar.
    @PostMapping("/callbacks/{orderId}/{token}")
    public ApiResponse callback(
            @PathVariable UUID orderId,
            @PathVariable String token) {

        if (!token.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Invalid callback token");
        }

        billing.callback(orderId.toString(), token);

        return new ApiResponse("Payment notification verified");
    }
}