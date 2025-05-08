package com.bitzomax.controller;

import com.bitzomax.entity.User;
import com.bitzomax.security.UserPrincipal;
import com.bitzomax.service.StripeService;
import com.bitzomax.service.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    @Value("${stripe.subscription.price.id}")
    private String subscriptionPriceId;

    @Autowired
    private UserService userService;

    @Autowired
    private StripeService stripeService;

    @PostMapping("/checkout-session")
    public ResponseEntity<?> createCheckoutSession(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isSubscribed()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You already have an active subscription"));
        }

        try {
            Session checkoutSession = stripeService.createCheckoutSession(
                    user.getEmail(),
                    subscriptionPriceId,
                    "http://localhost:4200/profile",
                    "http://localhost:4200/subscription/cancel"
            );

            return ResponseEntity.ok(Map.of(
                    "sessionId", checkoutSession.getId(),
                    "url", checkoutSession.getUrl()
            ));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSubscription(
            @RequestParam String sessionId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        try {
            User user = userService.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Session session = stripeService.retrieveCheckoutSession(sessionId);
            
            if ("complete".equals(session.getStatus())) {
                String customerId = session.getCustomer();
                String subscriptionId = session.getSubscription();
                
                User updatedUser = userService.updateSubscription(user, customerId, subscriptionId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("subscribed", true);
                response.put("subscriptionStartDate", updatedUser.getSubscriptionStartDate());
                response.put("subscriptionEndDate", updatedUser.getSubscriptionEndDate());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Checkout session is not complete"));
            }
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to confirm subscription: " + e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isSubscribed()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You don't have an active subscription"));
        }

        try {
            // Cancel the subscription in Stripe
            stripeService.cancelSubscription(user.getStripeSubscriptionId());
            
            // Update user record
            User updatedUser = userService.cancelSubscription(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscribed", false);
            response.put("message", "Your subscription has been canceled");
            
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel subscription: " + e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("subscribed", user.isSubscribed());
        
        if (user.isSubscribed()) {
            response.put("subscriptionStartDate", user.getSubscriptionStartDate());
            response.put("subscriptionEndDate", user.getSubscriptionEndDate());
            
            try {
                // Get the latest subscription status from Stripe
                com.stripe.model.Subscription subscription = 
                    stripeService.retrieveSubscription(user.getStripeSubscriptionId());
                
                response.put("stripeStatus", subscription.getStatus());
                response.put("currentPeriodEnd", subscription.getCurrentPeriodEnd());
            } catch (StripeException e) {
                // Just return the DB data if Stripe API fails
            }
        }

        return ResponseEntity.ok(response);
    }
}