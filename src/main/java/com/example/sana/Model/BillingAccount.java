package com.example.sana.Model;

import com.example.sana.Enums.BillingPlan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "billing_accounts")
@Getter @Setter
public class BillingAccount {
    @Id private Integer userId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BillingPlan planCode = BillingPlan.SPARK;
    private Instant currentPeriodEnd;
    @Column(length = 36) private String activeOrderId;
    @Version private Long version;
}
