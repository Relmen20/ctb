package com.copy.common.repository;

import com.copy.common.entity.AuthEntity;
import com.copy.common.entity.UserWalletsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWalletsRepository extends JpaRepository<UserWalletsEntity, Long> {
    List<UserWalletsEntity> findByAuthEntity(AuthEntity authId);
}
