package com.bitzomax.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate startDate;
    private LocalDate endDate;
    private boolean autoRenew;
    private String paymentId;

    @PrePersist
    protected void onCreate() {
        if (this.startDate == null) {
            this.startDate = LocalDate.now();
        }
        if (this.endDate == null) {
            this.endDate = this.startDate.plusMonths(1);
        }
    }
}