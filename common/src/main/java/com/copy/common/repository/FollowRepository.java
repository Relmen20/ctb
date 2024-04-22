package com.copy.common.repository;

import com.copy.common.entity.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<FollowEntity, Integer> {

    List<FollowEntity> findByUserChatId(Long chatId);

    FollowEntity findByFollowKeyWalletAndUserChatId(String followKeyWallet, Long chatId);
}
