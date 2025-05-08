import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { User } from './models/user.model';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  currentUser$: Observable<User | null>;
  isAuthenticated: boolean = false;
  isUploadModalOpen: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.currentUser$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.isAuthenticated = !!user;
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  onSearch(query: string): void {
    if (query.trim()) {
      this.router.navigate(['/videos'], { queryParams: { query } });
    }
  }

  toggleUploadModal(): void {
    // This will be implemented when we create the upload component
    this.isUploadModalOpen = !this.isUploadModalOpen;
    // For now, navigate to video upload page
    this.router.navigate(['/videos/upload']);
  }
}
