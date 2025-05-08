import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { VideoService } from '../../services/video.service';
import { AuthService } from '../../services/auth.service';
import { Video } from '../../models/video.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  isAuthenticated = false;
  trendingVideos: Video[] = [];
  isLoading = true;
  error: string | null = null;
  private subscriptions = new Subscription();

  constructor(
    private videoService: VideoService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.loadTrendingVideos();

    // Subscribe to auth status changes
    this.subscriptions.add(
      this.authService.isAuthenticated$.subscribe(status => {
        this.isAuthenticated = status;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  loadTrendingVideos(): void {
    this.isLoading = true;
    this.error = null;
    
    this.subscriptions.add(
      this.videoService.getVideos(0, 10, 'viewCount,desc').subscribe({
        next: (response) => {
          this.trendingVideos = response.content;
          this.isLoading = false;
        },
        error: (err) => {
          this.error = 'Failed to load trending videos. Please try again.';
          this.isLoading = false;
          console.error('Error loading trending videos:', err);
        }
      })
    );
  }

  navigateToSignup(): void {
    this.router.navigate(['/auth/signup']);
  }

  navigateToVideos(): void {
    this.router.navigate(['/videos']);
  }
}