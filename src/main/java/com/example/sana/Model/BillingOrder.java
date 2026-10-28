package com.example.sana.Model;

import com.example.sana.Enums.BillingPlan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "billing_orders", indexes = @Index(name = "idx_billing_order_user", columnList = "user_id"))
@Getter @Setter
public class BillingOrder {
    @Id @Column(length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BillingPlan planCode;
    @Column(nullable = false) private int amountHalalas;
    @Column(nullable = false, length = 3) private String currency = "SAR";
    @Column(nullable = false, length = 20) private String status = "CREATING";
    @Column(unique = true, length = 36) private String invoiceId;
    @Column(length = 1000) private String checkoutUrl;
    @Column(nullable = false, length = 64) private String callbackToken;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant expiresAt;
    private Instant paidAt;
    @Version private Long version;
}
