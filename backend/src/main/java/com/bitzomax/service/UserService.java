package com.bitzomax.service;

import com.bitzomax.dto.UserSummaryDTO;
import com.bitzomax.entity.User;
import com.bitzomax.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User createUser(String username, String email, String password, String fullName) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(User.Role.USER);
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateSubscription(User user, String stripeCustomerId, String stripeSubscriptionId) {
        user.setStripeCustomerId(stripeCustomerId);
        user.setStripeSubscriptionId(stripeSubscriptionId);
        user.setSubscribed(true);
        user.setSubscriptionStartDate(LocalDateTime.now());
        user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        return userRepository.save(user);
    }
    
    @Transactional
    public User cancelSubscription(User user) {
        user.setSubscribed(false);
        user.setSubscriptionEndDate(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public UserSummaryDTO convertToSummaryDTO(User user) {
        return new UserSummaryDTO(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getProfilePicture()
        );
    }
    
    @Transactional
    public User updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        try {
            User.Role role = User.Role.valueOf(roleName);
            user.setRole(role);
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role name: " + roleName);
        }
    }
    
    @Transactional
    public Map<String, Object> activateSubscription(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setSubscribed(true);
        user.setSubscriptionStartDate(LocalDateTime.now());
        user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
        User updatedUser = userRepository.save(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("user", updatedUser);
        return result;
    }
    
    @Transactional
    public Map<String, Object> cancelSubscription(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setSubscribed(false);
        user.setSubscriptionEndDate(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("user", updatedUser);
        return result;
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }
}