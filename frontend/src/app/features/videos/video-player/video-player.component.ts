import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { VideoService } from '../../../services/video.service';
import { Video } from '../../../models/video.model';
import { ENTER, COMMA } from '@angular/cdk/keycodes';

@Component({
  selector: 'app-video-player',
  templateUrl: './video-player.component.html',
  styleUrls: ['./video-player.component.scss']
})
export class VideoPlayerComponent implements OnInit, OnDestroy {
  @Input() videoId!: number; // Added ! to fix initialization error
  @Input() autoplay: boolean = false;
  @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>; // Added ! to fix initialization error
  
  video: Video | null = null;
  isLoading: boolean = true;
  error: string | null = null;
  
  // Playback state
  isPlaying: boolean = false;
  currentTime: number = 0;
  duration: number = 0;
  progress: number = 0;
  volume: number = 1;
  isPremium: boolean = false;
  previewTimeLimit: number = 30; // 30 seconds preview limit
  previewTimeReached: boolean = false;
  controlsVisible: boolean = true; // Changed name from showControls to avoid conflict with method
  
  // User state
  isAuthenticated: boolean = false;
  isSubscriber: boolean = false;
  
  private subscriptions: Subscription = new Subscription();
  private controlsTimer: any;
  private viewRecorded: boolean = false;
  
  constructor(
    public router: Router, // Changed from private to public to access in template
    private videoService: VideoService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Check authentication status
    this.subscriptions.add(
      this.authService.isAuthenticated$.subscribe(isAuth => {
        this.isAuthenticated = isAuth;
        
        if (isAuth) {
          this.subscriptions.add(
            this.authService.currentUser$.subscribe(user => {
              this.isSubscriber = user?.subscribed || false;
            })
          );
        }
      })
    );
    
    // Load the video
    if (this.videoId) {
      this.loadVideo();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.clearControlsTimer();
  }

  loadVideo(): void {
    this.isLoading = true;
    
    this.subscriptions.add(
      this.videoService.getVideo(this.videoId).subscribe({ // Changed getVideoById to getVideo
        next: (video: Video) => { // Added type annotation
          this.video = video;
          this.isPremium = video.premium;
          this.isLoading = false;
          
          // After video metadata loads, set up the player
          setTimeout(() => {
            if (this.videoElement && this.autoplay) {
              this.videoElement.nativeElement.play()
                .catch(err => console.error('Autoplay prevented:', err));
            }
          }, 100);
        },
        error: (err: any) => { // Added type annotation
          console.error('Error loading video:', err);
          this.error = 'Failed to load video. Please try again later.';
          this.isLoading = false;
        }
      })
    );
  }

  onVideoMetadataLoaded(): void {
    if (this.videoElement) {
      this.duration = this.videoElement.nativeElement.duration;
    }
  }

  onTimeUpdate(): void {
    if (this.videoElement) {
      this.currentTime = this.videoElement.nativeElement.currentTime;
      this.progress = (this.currentTime / this.duration) * 100;
      
      // Check for premium content time limit for non-subscribers
      if (this.isPremium && !this.isSubscriber && 
          this.currentTime >= this.previewTimeLimit && 
          !this.previewTimeReached) {
        this.previewTimeReached = true;
        this.pauseVideo();
        this.showSubscriptionPrompt();
      }
      
      // Record view once 10% of video is watched (or at least 10 seconds)
      const viewThreshold = Math.min(this.duration * 0.1, 10);
      if (!this.viewRecorded && this.currentTime >= viewThreshold) {
        this.recordView();
      }
    }
  }

  togglePlayPause(): void {
    if (this.videoElement) {
      if (this.videoElement.nativeElement.paused) {
        this.playVideo();
      } else {
        this.pauseVideo();
      }
    }
  }

  playVideo(): void {
    if (this.videoElement) {
      // Check for premium restriction before playing
      if (this.isPremium && !this.isSubscriber && this.previewTimeReached) {
        this.showSubscriptionPrompt();
        return;
      }
      
      this.videoElement.nativeElement.play()
        .then(() => {
          this.isPlaying = true;
        })
        .catch(err => {
          console.error('Error playing video:', err);
          this.snackBar.open('Error playing video. Please try again.', 'Close', { duration: 3000 });
        });
    }
  }

  pauseVideo(): void {
    if (this.videoElement) {
      this.videoElement.nativeElement.pause();
      this.isPlaying = false;
    }
  }

  seekVideo(event: MouseEvent): void {
    if (this.videoElement && this.duration) {
      const progressBar = event.currentTarget as HTMLElement;
      const rect = progressBar.getBoundingClientRect();
      const seekPercentage = (event.clientX - rect.left) / rect.width;
      
      // Calculate the seek time
      let seekTime = seekPercentage * this.duration;
      
      // Prevent seeking beyond preview limit for premium content
      if (this.isPremium && !this.isSubscriber) {
        seekTime = Math.min(seekTime, this.previewTimeLimit);
      }
      
      // Set the current time
      this.videoElement.nativeElement.currentTime = seekTime;
    }
  }

  setVolume(value: number): void {
    if (this.videoElement && !isNaN(value)) {
      this.volume = value;
      this.videoElement.nativeElement.volume = this.volume;
    }
  }

  toggleMute(): void {
    if (this.videoElement) {
      this.videoElement.nativeElement.muted = !this.videoElement.nativeElement.muted;
    }
  }

  toggleFullscreen(): void {
    if (!document.fullscreenElement) {
      this.videoElement.nativeElement.requestFullscreen()
        .catch(err => {
          console.error('Error attempting to enable fullscreen:', err);
        });
    } else {
      document.exitFullscreen();
    }
  }

  showControls(): void {
    this.controlsVisible = true; // Updated to use the new property name
    this.clearControlsTimer();
    
    // Hide controls after 3 seconds of inactivity
    this.controlsTimer = setTimeout(() => {
      if (this.isPlaying) {
        this.controlsVisible = false; // Updated to use the new property name
      }
    }, 3000);
  }

  clearControlsTimer(): void {
    if (this.controlsTimer) {
      clearTimeout(this.controlsTimer);
      this.controlsTimer = null;
    }
  }

  showSubscriptionPrompt(): void {
    if (this.isAuthenticated) {
      this.snackBar.open(
        'This is premium content. Subscribe to watch the full video.',
        'Subscribe Now',
        { duration: 6000 }
      ).onAction().subscribe(() => {
        this.router.navigate(['/subscription']);
      });
    } else {
      this.snackBar.open(
        'Sign in to watch premium content.',
        'Sign In',
        { duration: 6000 }
      ).onAction().subscribe(() => {
        this.router.navigate(['/auth/login'], { 
          queryParams: { returnUrl: `/videos/${this.videoId}` } 
        });
      });
    }
  }

  likeVideo(): void {
    if (!this.isAuthenticated) {
      this.snackBar.open('Sign in to like videos', 'Sign In', { duration: 3000 })
        .onAction().subscribe(() => {
          this.router.navigate(['/auth/login'], { 
            queryParams: { returnUrl: `/videos/${this.videoId}` } 
          });
        });
      return;
    }
    
    if (this.video && !this.video.likesCount) { // Changed liked to likesCount
      this.videoService.likeVideo(this.videoId).subscribe({
        next: () => {
          if (this.video) {
            // Update the UI state after liking
            this.video.likesCount = (this.video.likesCount || 0) + 1; // Changed likeCount to likesCount
          }
        },
        error: (err: any) => { // Added type annotation
          console.error('Error liking video:', err);
          this.snackBar.open('Error liking video', 'Close', { duration: 3000 });
        }
      });
    } else if (this.video && this.video.likesCount) { // Changed liked to likesCount
      this.videoService.unlikeVideo(this.videoId).subscribe({
        next: () => {
          if (this.video) {
            // Update the UI state after unliking
            this.video.likesCount = Math.max((this.video.likesCount || 1) - 1, 0); // Changed likeCount to likesCount
          }
        },
        error: (err: any) => { // Added type annotation
          console.error('Error unliking video:', err);
          this.snackBar.open('Error unliking video', 'Close', { duration: 3000 });
        }
      });
    }
  }

  recordView(): void {
    if (this.videoId && !this.viewRecorded) {
      this.viewRecorded = true;
      this.videoService.recordView(this.videoId).subscribe({
        error: (err) => {
          console.error('Error recording view:', err);
        }
      });
    }
  }

  formatVolumeLabel(value: number): string {
    return Math.round(value * 100) + '%';
  }
  
  formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    
    return `${minutes}:${secs < 10 ? '0' : ''}${secs}`;
  }
}
