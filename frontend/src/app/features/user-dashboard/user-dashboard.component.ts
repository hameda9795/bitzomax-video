import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';
import { environment } from '../../../environments/environment';

interface UserStats {
  videosWatched: number;
  watchTimeInHours: number;
  favorites: number;
  comments: number;
  commentsThisMonth: number;
}

interface ActivityItem {
  type: string;
  videoId: string;
  title: string;
  thumbnail: string;
  date: string;
}

@Component({
  selector: 'app-user-dashboard',
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.scss']
})
export class UserDashboardComponent implements OnInit {
  username = '';
  isAdmin = false;
  isSubscribed = false;
  subscriptionEnd: Date | null = null;
  userStats: UserStats = {
    videosWatched: 0,
    watchTimeInHours: 0,
    favorites: 0,
    comments: 0,
    commentsThisMonth: 0
  };
  recentActivity: ActivityItem[] = [];
  loading = true;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.username = user.username;
        this.isAdmin = user.role === 'ADMIN';
        this.isSubscribed = user.subscribed;
        this.subscriptionEnd = user.subscriptionEndDate ? new Date(user.subscriptionEndDate) : null;
        this.loadUserStats(user.id);
        this.loadRecentActivity(user.id);
      }
    });
  }

  loadUserStats(userId: number): void {
    this.http.get<UserStats>(`${environment.apiUrl}/users/${userId}/stats`)
      .subscribe({
        next: (data) => {
          this.userStats = data;
        },
        error: (err) => {
          console.error('Error loading user stats:', err);
          // Use mock data for now
          this.userStats = {
            videosWatched: 24,
            watchTimeInHours: 8.5,
            favorites: 12,
            comments: 7,
            commentsThisMonth: 3
          };
        }
      });
  }

  loadRecentActivity(userId: number): void {
    this.http.get<ActivityItem[]>(`${environment.apiUrl}/users/${userId}/activities?limit=5`)
      .subscribe({
        next: (data) => {
          this.recentActivity = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading recent activity:', err);
          // Use mock data for now
          this.recentActivity = [
            {
              type: 'Watched',
              videoId: '1',
              title: 'The Art of Poetry in Modern Times',
              thumbnail: 'assets/images/placeholder-thumbnail.jpg',
              date: new Date().toISOString()
            },
            {
              type: 'Liked',
              videoId: '2',
              title: 'Exploring Metaphors in Contemporary Poetry',
              thumbnail: 'assets/images/placeholder-thumbnail.jpg',
              date: new Date(Date.now() - 86400000).toISOString() // Yesterday
            },
            {
              type: 'Commented',
              videoId: '3',
              title: 'The Evolution of Poetry Throughout History',
              thumbnail: 'assets/images/placeholder-thumbnail.jpg',
              date: new Date(Date.now() - 172800000).toISOString() // 2 days ago
            }
          ];
          this.loading = false;
        }
      });
  }
}
