package com.bitzomax.repository;

import com.bitzomax.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findAllByStartDateBeforeAndEndDateAfter(LocalDate endBefore, LocalDate startAfter);
    long countByStartDateBetween(LocalDate start, LocalDate end);
}