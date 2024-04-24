package com.copy.common.entity;

import lombok.Data;

import javax.persistence.*;

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

    @Column(name = "sub_description", nullable = false)
    private String subDescription;

    @Column(name = "follow_key_available", nullable = false)
    private Integer followKeyAvailable;

    @Column(name = "count_autotrade_available", nullable = false)
    private Integer countAutotradeAvailable;

    @Column(name = "sub_price", nullable = false)
    private Float subPrice;

    @Column(name = "sub_date_period", nullable = false)
    private String subDatePeriod;

//    public Period getSubDatePeriodAsObject() {
//        return Period.parse(subDatePeriod);
//    }
//
//    public void setSubDatePeriodAsObject(Period period) {
//        this.subDatePeriod = period.toString();
//    }
}
