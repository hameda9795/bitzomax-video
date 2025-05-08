package com.bitzomax.controller.admin;

import com.bitzomax.entity.User;
import com.bitzomax.security.UserPrincipal;
import com.bitzomax.service.StatsService;
import com.bitzomax.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private StatsService statsService;

    @Autowired
    private UserService userService;

    @GetMapping("/stats/summary")
    public ResponseEntity<?> getStatsSummary() {
        Map<String, Object> response = new HashMap<>();
        
        // Get stats from service
        response.put("totalUsers", statsService.getTotalUsers());
        response.put("subscribedUsers", statsService.getSubscribedUsers());
        response.put("totalVideos", statsService.getTotalVideos());
        response.put("premiumVideos", statsService.getPremiumVideos());
        response.put("totalViews", statsService.getTotalViews());
        response.put("totalLikes", statsService.getTotalLikes());
        response.put("revenueThisMonth", statsService.getRevenueThisMonth());
        response.put("percentGrowth", statsService.getRevenueGrowthPercentage());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/user-growth")
    public ResponseEntity<?> getUserGrowthStats() {
        Map<String, Object> response = new HashMap<>();
        
        // Get last 6 months data
        List<String> labels = new ArrayList<>();
        List<Long> users = new ArrayList<>();
        List<Long> subscribers = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            labels.add(month.format(formatter));
            
            // Get data for this month
            users.add(statsService.getUsersForMonth(month.getMonthValue(), month.getYear()));
            subscribers.add(statsService.getSubscribersForMonth(month.getMonthValue(), month.getYear()));
        }
        
        response.put("labels", labels);
        response.put("users", users);
        response.put("subscribers", subscribers);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/engagement")
    public ResponseEntity<?> getEngagementStats() {
        Map<String, Object> response = new HashMap<>();
        
        // Get last 6 months data
        List<String> labels = new ArrayList<>();
        List<Long> views = new ArrayList<>();
        List<Long> likes = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            labels.add(month.format(formatter));
            
            // Get data for this month
            views.add(statsService.getViewsForMonth(month.getMonthValue(), month.getYear()));
            likes.add(statsService.getLikesForMonth(month.getMonthValue(), month.getYear()));
        }
        
        response.put("labels", labels);
        response.put("views", views);
        response.put("likes", likes);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<?> getRevenueStats() {
        Map<String, Object> response = new HashMap<>();
        
        // Get last 12 months data
        List<String> labels = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");
        
        for (int i = 11; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            labels.add(month.format(formatter));
            
            // Get data for this month
            revenue.add(statsService.getRevenueForMonth(month.getMonthValue(), month.getYear()));
        }
        
        response.put("labels", labels);
        response.put("revenue", revenue);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        // Prevent self-demotion
        if (userId.equals(currentUser.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You cannot change your own role"));
        }
        
        String newRole = payload.get("role");
        if (newRole == null || (!newRole.equals("ADMIN") && !newRole.equals("USER"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid role specified"));
        }
        
        User updatedUser = userService.updateUserRole(userId, newRole);
        return ResponseEntity.ok(Map.of("success", true, "user", updatedUser));
    }
    
    @PutMapping("/users/{userId}/subscription")
    public ResponseEntity<?> updateUserSubscription(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> payload) {
        
        Boolean subscribed = payload.get("subscribed");
        if (subscribed == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Subscription status not specified"));
        }
        
        Map<String, Object> result;
        
        if (subscribed) {
            result = userService.activateSubscription(userId);
        } else {
            result = userService.cancelSubscription(userId);
        }
        
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, @AuthenticationPrincipal UserPrincipal currentUser) {
        // Prevent self-deletion
        if (userId.equals(currentUser.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You cannot delete your own account"));
        }
        
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}