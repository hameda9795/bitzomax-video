import { Injectable } from '@angular/core';
import { Router, CanActivate, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  
  constructor(private authService: AuthService, private router: Router) {}
  
  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.authService.currentUser$.pipe(
      take(1), 
      map(user => {
        // If user is logged in, allow access
        if (user) {
          return true;
        }
        
        // If not logged in, redirect to login with return URL
        return this.router.createUrlTree(['/auth/login'], { 
          queryParams: { returnUrl: this.router.url } 
        });
      })
    );
  }
}