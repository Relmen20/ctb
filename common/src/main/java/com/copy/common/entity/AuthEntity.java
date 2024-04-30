package com.copy.common.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "auth", schema = "trader")
@Data
public class AuthEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @Column(name = "person_name", nullable = false)
    private String personName;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @ManyToOne
    @JoinColumn(name = "active_sub_id", nullable = false, referencedColumnName = "subscription_id")
    private SubscriptionEntity subscriptionEntity;

    @Column(name = "sub_start_date", nullable = false)
    private LocalDate subStartDate;

//    @OneToMany(mappedBy = "followEntity", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<FollowEntity> followEntities;
//
//    @OneToMany(mappedBy = "userWalletEntity", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JoinColumn
//    private List<UserWalletsEntity> wallets;
}
