package com.bitzomax.service;

import com.bitzomax.repository.UserRepository;
import com.bitzomax.repository.VideoRepository;
import com.bitzomax.repository.LikeRepository;
import com.bitzomax.repository.VideoViewRepository;
import com.bitzomax.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class StatsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private VideoViewRepository viewRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getSubscribedUsers() {
        return userRepository.countBySubscribedTrue();
    }

    public long getTotalVideos() {
        return videoRepository.count();
    }

    public long getPremiumVideos() {
        return videoRepository.countByPremiumTrue();
    }

    public long getTotalViews() {
        return viewRepository.count();
    }

    public long getTotalLikes() {
        return likeRepository.count();
    }

    public double getRevenueThisMonth() {
        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.of(now.getYear(), now.getMonth());
        
        // Get the first and last day of the current month
        LocalDate firstDay = currentMonth.atDay(1);
        LocalDate lastDay = currentMonth.atEndOfMonth();
        
        // Get all subscriptions that were active this month
        return subscriptionRepository.findAllByStartDateBeforeAndEndDateAfter(lastDay, firstDay)
                                     .stream()
                                     .mapToDouble(sub -> 6.0) // Each subscription is €6
                                     .sum();
    }

    public double getRevenueGrowthPercentage() {
        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.of(now.getYear(), now.getMonth());
        YearMonth lastMonth = currentMonth.minusMonths(1);
        
        double thisMonthRevenue = getRevenueForMonth(currentMonth.getMonthValue(), currentMonth.getYear());
        double lastMonthRevenue = getRevenueForMonth(lastMonth.getMonthValue(), lastMonth.getYear());
        
        if (lastMonthRevenue == 0) {
            return thisMonthRevenue > 0 ? 100.0 : 0.0;
        }
        
        return ((thisMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100.0;
    }

    public double getRevenueForMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // Get the first and last day of the given month
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        // Get all subscriptions that were active that month
        return subscriptionRepository.findAllByStartDateBeforeAndEndDateAfter(lastDay, firstDay)
                                     .stream()
                                     .mapToDouble(sub -> 6.0) // Each subscription is €6
                                     .sum();
    }

    public long getUsersForMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // Get the first and last day of the given month
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        return userRepository.countByCreatedAtBetween(firstDay.atStartOfDay(), lastDay.atTime(23, 59, 59));
    }

    public long getSubscribersForMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // Get the first and last day of the given month
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        return subscriptionRepository.countByStartDateBetween(firstDay, lastDay);
    }

    public long getViewsForMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // Get the first and last day of the given month
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        return viewRepository.countByViewedAtBetween(firstDay.atStartOfDay(), lastDay.atTime(23, 59, 59));
    }

    public long getLikesForMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // Get the first and last day of the given month
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        
        return likeRepository.countByCreatedAtBetween(firstDay.atStartOfDay(), lastDay.atTime(23, 59, 59));
    }
}