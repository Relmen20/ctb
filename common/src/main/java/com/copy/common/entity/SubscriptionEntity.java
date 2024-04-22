package com.copy.common.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.Period;

@Entity
@Table(name = "subscription", schema = "trader")
@Data
public class SubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "sub_name", nullable = false)
    private String subName;

    @Column(name = "follow_key_available", nullable = false)
    private Integer followKeyAvailable;

    @Column(name = "count_autotrade_available", nullable = false)
    private Integer countAutotradeAvailable;

    @Column(name = "sub_price", nullable = false)
    private Float subPrice;

    @Column(name = "sub_date_period", nullable = false)
    private Period subDatePeriod;
}
