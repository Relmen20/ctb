package com.copy.common.entity;


import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "subscription_history", schema = "trader")
@Data
public class SubscriptionHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long tradeId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "auth_id")
    private AuthEntity user;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false, referencedColumnName = "subscription_id")
    private SubscriptionEntity subscriptionEntity;

    @Column(name = "status", nullable = false)
    private Boolean status;
}
