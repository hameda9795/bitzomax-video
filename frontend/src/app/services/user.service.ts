import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ProfileUpdate, User, UserProfile } from '../models/user.model';
import { Video, VideoResponse } from '../models/video.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  getUserProfile(username: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${username}`).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to get user profile'));
      })
    );
  }

  updateProfile(profileUpdate: ProfileUpdate): Observable<User> {
    const formData = new FormData();
    
    if (profileUpdate.fullName) {
      formData.append('fullName', profileUpdate.fullName);
    }
    
    if (profileUpdate.bio) {
      formData.append('bio', profileUpdate.bio);
    }
    
    if (profileUpdate.profilePicture) {
      formData.append('profilePicture', profileUpdate.profilePicture);
    }

    return this.http.put<User>(`${this.apiUrl}/me`, formData).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to update profile'));
      })
    );
  }

  getUserVideos(username: string, page: number = 0, size: number = 10): Observable<VideoResponse> {
    return this.http.get<VideoResponse>(`${this.apiUrl}/${username}/videos?page=${page}&size=${size}`).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to get user videos'));
      })
    );
  }

  getFavoriteVideos(page: number = 0, size: number = 10): Observable<VideoResponse> {
    return this.http.get<VideoResponse>(`${this.apiUrl}/me/favorites?page=${page}&size=${size}`).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to get favorite videos'));
      })
    );
  }

  getWatchLaterVideos(page: number = 0, size: number = 10): Observable<VideoResponse> {
    return this.http.get<VideoResponse>(`${this.apiUrl}/me/watch-later?page=${page}&size=${size}`).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to get watch later videos'));
      })
    );
  }

  getWatchHistory(page: number = 0, size: number = 10): Observable<VideoResponse> {
    return this.http.get<VideoResponse>(`${this.apiUrl}/me/history?page=${page}&size=${size}`).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to get watch history'));
      })
    );
  }

  clearWatchHistory(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/me/history`).pipe(
      catchError(error => {
        return throwError(() => new Error(error.error?.message || 'Failed to clear watch history'));
      })
    );
  }
}