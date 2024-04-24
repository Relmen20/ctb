package com.copy.common.entity;


import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "client_wallets", schema = "trader")
@Data
public class UserWalletsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Integer walletId;

    @ManyToOne
    @JoinColumn(name = "auth_id", nullable = false, referencedColumnName = "auth_id")
    private AuthEntity authEntity;

    @Column(name = "wallet_address", nullable = false)
    private String walletAddress;

    @Column(name = "private_key", nullable = false)
    private String privateKey;
}
