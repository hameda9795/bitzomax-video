import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { CheckoutSession } from '../models/auth.model';
import { SubscriptionInfo } from '../models/user.model';
import { AuthService } from './auth.service';

declare var Stripe: any;

@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private apiUrl = `${environment.apiUrl}/subscriptions`;
  private stripe: any;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    this.stripe = Stripe(environment.stripePublishableKey);
  }

  createSubscription(): Observable<CheckoutSession> {
    const returnUrl = `${window.location.origin}/subscription/success`;
    
    return this.http.post<CheckoutSession>(`${this.apiUrl}/create-checkout-session`, { returnUrl }).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to create subscription'));
      })
    );
  }

  redirectToCheckout(sessionId: string): Promise<any> {
    return this.stripe.redirectToCheckout({ sessionId });
  }

  getSubscriptionStatus(): Observable<SubscriptionInfo> {
    return this.http.get<SubscriptionInfo>(`${this.apiUrl}/status`).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to get subscription status'));
      })
    );
  }

  cancelSubscription(): Observable<SubscriptionInfo> {
    return this.http.post<SubscriptionInfo>(`${this.apiUrl}/cancel`, {}).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to cancel subscription'));
      })
    );
  }

  resumeSubscription(): Observable<SubscriptionInfo> {
    return this.http.post<SubscriptionInfo>(`${this.apiUrl}/resume`, {}).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to resume subscription'));
      })
    );
  }
}