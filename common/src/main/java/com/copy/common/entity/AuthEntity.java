package com.copy.common.entity;

import lombok.Data;
import javax.persistence.*;

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

    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(name = "private_key")
    private String privateKey;
}
