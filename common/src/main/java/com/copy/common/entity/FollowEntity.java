package com.copy.common.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "follow", schema = "trader")
@Data
public class FollowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId;

    @ManyToOne
    @JoinColumn(name = "auth_id", nullable = false, referencedColumnName = "auth_id")
    private AuthEntity authEntity;

    @Column(name = "follow_key_wallet", nullable = false)
    private String followKeyWallet;

    @Column(name = "name_of_wallet", nullable = false)
    private String nameOfWallet;

    @Column(name = "count_coll_done", nullable = false)
    private Integer countCollDone = 0;

    @Column(name = "count_autotrade_done", nullable = false)
    private Integer countAutotradeDone = 0;

    @Column(name = "date_start_follow", nullable = false)
    private LocalDate dateStartFollow;

    @Column(name = "tracking_status", nullable = false)
    private Boolean trackingStatus;
}
