import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { map, take } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  
  constructor(
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> {
    return this.authService.currentUser$.pipe(
      take(1),
      map(user => {
        // Check if the user is logged in and has admin role
        const isAdmin = !!user && user.role === 'ADMIN';
        
        if (!isAdmin) {
          this.snackBar.open('Access denied. Admin privileges required.', 'Close', {
            duration: 5000
          });
          this.router.navigate(['/']);
        }
        
        return isAdmin;
      })
    );
  }
}