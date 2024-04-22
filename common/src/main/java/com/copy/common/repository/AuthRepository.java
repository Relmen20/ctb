package com.copy.common.repository;

import com.copy.common.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<AuthEntity, Integer> {

    Optional<AuthEntity> findByChatId(Long chatId);
}
