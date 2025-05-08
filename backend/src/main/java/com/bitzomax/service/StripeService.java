package com.bitzomax.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Creates a Stripe checkout session for subscription
     *
     * @param customerEmail Customer's email
     * @param priceId The price ID for the subscription
     * @param successUrl URL to redirect on successful payment
     * @param cancelUrl URL to redirect on canceled payment
     * @return Created checkout session
     * @throws StripeException if Stripe API call fails
     */
    public Session createCheckoutSession(
            String customerEmail, String priceId, String successUrl, String cancelUrl) throws StripeException {
        
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        lineItems.add(
                SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
        );

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(customerEmail)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addAllLineItem(lineItems)
                .build();

        return Session.create(params);
    }

    /**
     * Retrieves a checkout session by ID
     *
     * @param sessionId The checkout session ID
     * @return The checkout session
     * @throws StripeException if Stripe API call fails
     */
    public Session retrieveCheckoutSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    /**
     * Cancels a subscription in Stripe
     *
     * @param subscriptionId The subscription ID to cancel
     * @throws StripeException if Stripe API call fails
     */
    public void cancelSubscription(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);
        
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();
        
        subscription.update(params);
    }

    /**
     * Retrieves a subscription from Stripe
     *
     * @param subscriptionId The subscription ID to retrieve
     * @return The subscription object
     * @throws StripeException if Stripe API call fails
     */
    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

    /**
     * Creates a subscription directly (without checkout)
     *
     * @param customerId The customer ID
     * @param priceId The price ID for the subscription
     * @return The created subscription
     * @throws StripeException if Stripe API call fails
     */
    public Subscription createSubscription(String customerId, String priceId) throws StripeException {
        Map<String, Object> item = new HashMap<>();
        item.put("price", priceId);

        Map<String, Object> items = new HashMap<>();
        items.put("0", item);

        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        params.put("items", items);

        return Subscription.create(params);
    }
}