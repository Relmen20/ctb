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
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "auth_id")
    private AuthEntity user;

    @Column(name = "follow_key_wallet", nullable = false)
    private String followKeyWallet;

    @Column(name = "count_coll_done")
    private Integer countCollDone;

//    @Column()

    @Column(name = "count_autotrade_done")
    private Integer countAutotradeDone;

    @Column(name = "date_start_follow", nullable = false)
    private LocalDate dateStartFollow;

}
