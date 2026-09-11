package com.bridge.bdbank.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA pour l'historique des notifications envoyées.
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    Page<NotificationHistory> findByRuleIdOrderBySentAtDesc(Long ruleId, Pageable pageable);

    Page<NotificationHistory> findAllByOrderBySentAtDesc(Pageable pageable);
}
