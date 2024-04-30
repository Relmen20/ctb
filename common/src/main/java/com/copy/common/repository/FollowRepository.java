package com.copy.common.repository;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, Long> {

    List<FollowEntity> findByAuthEntity(AuthEntity authId);

    Optional<FollowEntity> findByFollowKeyWallet(String keyWallet);
}
