package com.copy.common.repository;

import com.copy.common.entity.SubscriptionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistoryEntity, Integer> {
}
