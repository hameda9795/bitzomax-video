import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { VideoService } from 'src/app/services/video.service';
import { Video } from 'src/app/models/video.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-video-management',
  templateUrl: './video-management.component.html',
  styleUrls: ['./video-management.component.scss']
})
export class VideoManagementComponent implements OnInit {
  displayedColumns: string[] = ['thumbnail', 'title', 'uploader', 'premium', 'views', 'likes', 'createdAt', 'actions'];
  dataSource: MatTableDataSource<Video> = new MatTableDataSource<Video>([]);
  videos: Video[] = [];
  isLoading = true;
  error: string | null = null;
  totalVideos = 0;
  
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private videoService: VideoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadVideos();
  }

  loadVideos(page: number = 0, size: number = 10): void {
    this.isLoading = true;
    this.error = null;
    
    this.videoService.getAdminVideos(page, size).subscribe({
      next: (response) => {
        this.videos = response.content;
        this.totalVideos = response.totalElements;
        this.dataSource = new MatTableDataSource(this.videos);
        this.dataSource.sort = this.sort;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading videos:', error);
        this.error = 'Failed to load videos. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any): void {
    this.loadVideos(event.pageIndex, event.pageSize);
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  editVideo(id: number): void {
    this.router.navigate(['/admin/videos/edit', id]);
  }

  deleteVideo(id: number, title: string): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '450px',
      data: {
        title: 'Delete Video',
        message: `Are you sure you want to delete "${title}"? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.videoService.deleteVideo(id).subscribe({
          next: () => {
            this.snackBar.open('Video deleted successfully', 'Close', {
              duration: 3000
            });
            this.loadVideos(this.paginator.pageIndex, this.paginator.pageSize);
          },
          error: (error) => {
            console.error('Error deleting video:', error);
            this.snackBar.open('Failed to delete video. Please try again.', 'Close', {
              duration: 5000
            });
          }
        });
      }
    });
  }

  togglePremiumStatus(video: Video): void {
    const newStatus = !video.premium;
    
    this.videoService.togglePremiumStatus(video.id).subscribe({
      next: () => {
        video.premium = newStatus;
        this.snackBar.open(
          `Video set to ${newStatus ? 'premium' : 'free'} successfully`,
          'Close',
          { duration: 3000 }
        );
      },
      error: (error: any) => {
        console.error('Error updating video status:', error);
        this.snackBar.open('Failed to update video status. Please try again.', 'Close', {
          duration: 5000
        });
      }
    });
  }

  uploadNewVideo(): void {
    this.router.navigate(['/admin/videos/upload']);
  }

  viewAnalytics(id: number): void {
    this.router.navigate(['/admin/videos/analytics', id]);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString();
  }
}
