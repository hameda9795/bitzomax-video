import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PagedVideos, Video, VideoComment, VideoStatistics, VideoUpload } from '../models/video.model';

@Injectable({
  providedIn: 'root'
})
export class VideoService {
  private apiUrl = `${environment.apiUrl}/api/videos`;
  private adminApiUrl = `${environment.apiUrl}/api/admin/videos`;
  
  constructor(private http: HttpClient) { }
  
  // Public video endpoints
  
  getVideos(page = 0, size = 10, sort = 'uploadDate,desc', filter?: string): Observable<PagedVideos> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
      
    if (filter) {
      params = params.set('filter', filter);
    }
    
    return this.http.get<PagedVideos>(this.apiUrl, { params });
  }
  
  getVideo(id: number): Observable<Video> {
    return this.http.get<Video>(`${this.apiUrl}/${id}`);
  }
  
  getRelatedVideos(id: number, limit = 6): Observable<Video[]> {
    return this.http.get<Video[]>(`${this.apiUrl}/${id}/related?limit=${limit}`);
  }
  
  incrementViewCount(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/view`, {});
  }
  
  // Add recordView method as an alias for incrementViewCount
  recordView(id: number): Observable<void> {
    return this.incrementViewCount(id);
  }
  
  likeVideo(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/like`, {});
  }
  
  unlikeVideo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/like`);
  }
  
  getComments(videoId: number): Observable<VideoComment[]> {
    return this.http.get<VideoComment[]>(`${this.apiUrl}/${videoId}/comments`);
  }
  
  addComment(videoId: number, text: string): Observable<VideoComment> {
    return this.http.post<VideoComment>(`${this.apiUrl}/${videoId}/comments`, { text });
  }
  
  searchVideos(query: string, page = 0, size = 10): Observable<PagedVideos> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
      
    return this.http.get<PagedVideos>(`${this.apiUrl}/search`, { params });
  }
  
  getVideosByTag(tag: string, page = 0, size = 10): Observable<PagedVideos> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
      
    return this.http.get<PagedVideos>(`${this.apiUrl}/tags/${tag}`, { params });
  }
  
  // Admin video endpoints
  
  getAdminVideos(page = 0, size = 10, sort = 'uploadDate,desc', search?: string, premium?: boolean): Observable<PagedVideos> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
      
    if (search) {
      params = params.set('search', search);
    }
    
    if (premium !== undefined) {
      params = params.set('premium', premium.toString());
    }
    
    return this.http.get<PagedVideos>(this.adminApiUrl, { params });
  }
  
  uploadVideo(videoData: VideoUpload, videoFile: File, thumbnailFile: File): Observable<HttpEvent<Video>> {
    const formData = new FormData();
    
    // Append video metadata as JSON string
    formData.append('metadata', new Blob([JSON.stringify(videoData)], { type: 'application/json' }));
    
    // Append files
    formData.append('video', videoFile);
    formData.append('thumbnail', thumbnailFile);
    
    const req = new HttpRequest('POST', this.adminApiUrl + '/upload', formData, {
      reportProgress: true
    });
    
    return this.http.request<Video>(req);
  }
  
  updateVideo(id: number, videoData: VideoUpload): Observable<Video> {
    return this.http.put<Video>(`${this.adminApiUrl}/${id}`, videoData);
  }
  
  updateThumbnail(id: number, thumbnailFile: File): Observable<void> {
    const formData = new FormData();
    formData.append('thumbnail', thumbnailFile);
    
    return this.http.post<void>(`${this.adminApiUrl}/${id}/thumbnail`, formData);
  }
  
  deleteVideo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminApiUrl}/${id}`);
  }
  
  getVideoStatistics(): Observable<VideoStatistics> {
    return this.http.get<VideoStatistics>(`${this.adminApiUrl}/statistics`);
  }
  
  convertToWebM(videoFile: File): Observable<{video: Blob}> {
    const formData = new FormData();
    formData.append('video', videoFile);
    
    return this.http.post<{video: Blob}>(`${this.adminApiUrl}/convert-webm`, formData, {
      responseType: 'blob' as 'json'
    });
  }
  
  updateVideoProperty(id: number, property: string, value: any): Observable<Video> {
    return this.http.patch<Video>(`${this.adminApiUrl}/${id}/${property}`, { value });
  }
  
  togglePremiumStatus(id: number): Observable<Video> {
    return this.http.patch<Video>(`${this.adminApiUrl}/${id}/toggle-premium`, {});
  }
}