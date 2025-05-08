import { Component, OnInit } from '@angular/core';
import { ChartOptions, ChartType, ChartDataset } from 'chart.js';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface StatsSummary {
  totalUsers: number;
  subscribedUsers: number;
  totalVideos: number;
  premiumVideos: number;
  totalLikes: number;
  totalViews: number;
  revenueThisMonth: number;
  percentGrowth: number;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  
  stats: StatsSummary = {
    totalUsers: 0,
    subscribedUsers: 0,
    totalVideos: 0,
    premiumVideos: 0,
    totalLikes: 0,
    totalViews: 0,
    revenueThisMonth: 0,
    percentGrowth: 0
  };
  isLoading = true;
  error: string | null = null;
  
  // User Growth Chart
  userChartLabels: string[] = [];
  userChartType: ChartType = 'line';
  userChartData: ChartDataset[] = [
    { data: [], label: 'New Users', backgroundColor: 'rgba(255, 221, 0, 0.2)', borderColor: '#FFDD00', tension: 0.4 },
    { data: [], label: 'New Subscribers', backgroundColor: 'rgba(43, 127, 255, 0.2)', borderColor: '#2B7FFF', tension: 0.4 }
  ];
  userChartOptions: ChartOptions = {
    responsive: true,
    plugins: {
      title: {
        display: true,
        text: 'User Growth'
      },
      tooltip: {
        mode: 'index',
        intersect: false,
      }
    }
  };
  
  // Content Engagement Chart
  engagementChartLabels: string[] = [];
  engagementChartType: ChartType = 'bar';
  engagementChartData: ChartDataset[] = [
    { data: [], label: 'Views', backgroundColor: '#FFDD00' },
    { data: [], label: 'Likes', backgroundColor: '#FF4B4B' }
  ];
  engagementChartOptions: ChartOptions = {
    responsive: true,
    plugins: {
      title: {
        display: true,
        text: 'Content Engagement'
      }
    }
  };
  
  // Revenue Chart
  revenueChartLabels: string[] = [];
  revenueChartType: ChartType = 'line';
  revenueChartData: ChartDataset[] = [
    { data: [], label: 'Monthly Revenue (€)', backgroundColor: 'rgba(43, 127, 255, 0.2)', borderColor: '#2B7FFF', fill: true }
  ];
  revenueChartOptions: ChartOptions = {
    responsive: true,
    plugins: {
      title: {
        display: true,
        text: 'Subscription Revenue'
      }
    }
  };
  
  constructor(private http: HttpClient) {}
  
  ngOnInit(): void {
    this.loadStatsSummary();
    this.loadUserGrowthData();
    this.loadEngagementData();
    this.loadRevenueData();
  }
  
  loadStatsSummary(): void {
    this.http.get<StatsSummary>(`${environment.apiUrl}/admin/stats/summary`)
      .subscribe({
        next: (data) => {
          this.stats = data;
          this.isLoading = false;
        },
        error: (err) => {
          this.error = 'Failed to load statistics. Please try again later.';
          this.isLoading = false;
          console.error('Error loading stats summary:', err);
        }
      });
  }
  
  loadUserGrowthData(): void {
    this.http.get<{labels: string[], users: number[], subscribers: number[]}>(`${environment.apiUrl}/admin/stats/user-growth`)
      .subscribe({
        next: (data) => {
          this.userChartLabels = data.labels;
          this.userChartData[0].data = data.users;
          this.userChartData[1].data = data.subscribers;
        },
        error: (err) => {
          console.error('Error loading user growth data:', err);
        }
      });
  }
  
  loadEngagementData(): void {
    this.http.get<{labels: string[], views: number[], likes: number[]}>(`${environment.apiUrl}/admin/stats/engagement`)
      .subscribe({
        next: (data) => {
          this.engagementChartLabels = data.labels;
          this.engagementChartData[0].data = data.views;
          this.engagementChartData[1].data = data.likes;
        },
        error: (err) => {
          console.error('Error loading engagement data:', err);
        }
      });
  }
  
  loadRevenueData(): void {
    this.http.get<{labels: string[], revenue: number[]}>(`${environment.apiUrl}/admin/stats/revenue`)
      .subscribe({
        next: (data) => {
          this.revenueChartLabels = data.labels;
          this.revenueChartData[0].data = data.revenue;
        },
        error: (err) => {
          console.error('Error loading revenue data:', err);
        }
      });
  }
}
