import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest, SignupRequest } from '../models/auth.model';
import { User } from '../models/user.model';
import { JwtHelperService } from '@auth0/angular-jwt';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private currentUserSubject: BehaviorSubject<User | null>;
  private authStatusSubject: BehaviorSubject<boolean>;
  public currentUser$: Observable<User | null>;
  public isAuthenticated$: Observable<boolean>;
  private jwtHelper = new JwtHelperService();

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<User | null>(storedUser ? JSON.parse(storedUser) : null);
    this.currentUser$ = this.currentUserSubject.asObservable();
    
    // Initialize auth status subject based on token validity
    this.authStatusSubject = new BehaviorSubject<boolean>(this.isAuthenticated());
    this.isAuthenticated$ = this.authStatusSubject.asObservable();
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  login(loginRequest: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, loginRequest)
      .pipe(
        tap(response => {
          this.storeAuthResponse(response);
          this.authStatusSubject.next(true);
        }),
        catchError(error => {
          return throwError(() => new Error(error.error?.message || 'Login failed'));
        })
      );
  }

  signup(signupRequest: SignupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/signup`, signupRequest)
      .pipe(
        tap(response => {
          this.storeAuthResponse(response);
          this.authStatusSubject.next(true);
        }),
        catchError(error => {
          return throwError(() => new Error(error.error?.message || 'Registration failed'));
        })
      );
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('token');
    this.currentUserSubject.next(null);
    this.authStatusSubject.next(false);
  }

  isAuthenticated(): boolean {
    const token = localStorage.getItem('token');
    return !!token && !this.jwtHelper.isTokenExpired(token);
  }

  isSubscribed(): boolean {
    const user = this.currentUserValue;
    return !!user && user.subscribed;
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  private storeAuthResponse(response: AuthResponse): void {
    localStorage.setItem('token', response.token);
    
    const user: User = {
      id: response.id,
      username: response.username,
      email: response.email,
      fullName: '', // This would normally be included in the response
      role: response.role as 'USER' | 'ADMIN',
      subscribed: response.subscribed
    };
    
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }
}