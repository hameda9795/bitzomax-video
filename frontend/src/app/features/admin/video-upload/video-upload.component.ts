import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { HttpEventType, HttpResponse } from '@angular/common/http';
import { VideoService } from '../../../services/video.service';
import { Video, VideoUpload } from '../../../models/video.model';
import { finalize } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Location } from '@angular/common';

@Component({
  selector: 'app-video-upload',
  templateUrl: './video-upload.component.html',
  styleUrls: ['./video-upload.component.scss']
})
export class VideoUploadComponent implements OnInit {
  uploadForm: FormGroup;
  isLoading = false;
  isEditing = false;
  videoId: number | null = null;
  
  // File handling
  videoFile: File | null = null;
  thumbnailFile: File | null = null;
  previewUrl: string | null = null;
  thumbnailUrl: string | null = null;
  
  // Web worker for conversion
  private webWorker: Worker | null = null;
  isConverting = false;
  conversionProgress = 0;
  
  // Error handling
  error: string | null = null;
  
  // Chip input
  readonly separatorKeysCodes = [ENTER, COMMA] as const;
  tags: string[] = [];
  
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('thumbnailInput') thumbnailInput!: ElementRef<HTMLInputElement>;
  @ViewChild('videoPreview') videoPreview!: ElementRef<HTMLVideoElement>;
  
  constructor(
    private fb: FormBuilder,
    private videoService: VideoService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private location: Location
  ) {
    this.uploadForm = this.createForm();
  }

  ngOnInit(): void {
    // Check if we're in edit mode
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.videoId = +id;
        this.isEditing = true;
        this.loadVideo(this.videoId);
      }
    });
    
    // Initialize web worker if browser supports
    if (typeof Worker !== 'undefined') {
      this.webWorker = new Worker('/assets/workers/video-converter.js');
      this.setupWebWorkerListeners();
    }
  }
  
  ngOnDestroy(): void {
    if (this.webWorker) {
      this.webWorker.terminate();
    }
    
    // Clean up object URLs
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }
    if (this.thumbnailUrl) {
      URL.revokeObjectURL(this.thumbnailUrl);
    }
  }
  
  private createForm(): FormGroup {
    return this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
      poemText: [''],
      premium: [false],
      seoTitle: [''],
      seoDescription: [''],
      seoKeywords: ['']
    });
  }
  
  private loadVideo(id: number): void {
    this.isLoading = true;
    
    this.videoService.getVideo(id)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (video) => {
          this.uploadForm.patchValue({
            title: video.title,
            description: video.description,
            poemText: video.poemText || '',
            premium: video.premium,
            seoTitle: video.seoTitle || '',
            seoDescription: video.seoDescription || '',
            seoKeywords: video.seoKeywords || ''
          });
          
          this.tags = [...video.tags];
          this.previewUrl = video.videoUrl;
          this.thumbnailUrl = video.thumbnailUrl;
        },
        error: (err) => {
          this.error = 'Failed to load video: ' + (err.error?.message || err.statusText);
          this.snackBar.open('Error loading video', 'Dismiss', { duration: 5000 });
        }
      });
  }
  
  browseVideo(): void {
    this.fileInput.nativeElement.click();
  }
  
  browseThumbnail(): void {
    this.thumbnailInput.nativeElement.click();
  }
  
  onFileSelected(event: Event, type: 'video' | 'thumbnail'): void {
    const element = event.target as HTMLInputElement;
    if (element.files && element.files.length) {
      const file = element.files[0];
      
      // Handle file based on type
      if (type === 'video') {
        this.handleVideoFile(file);
      } else {
        this.handleThumbnailFile(file);
      }
    }
  }
  
  private handleVideoFile(file: File): void {
    // Validate file type
    if (!file.type.includes('video/')) {
      this.error = 'Please select a valid video file';
      this.snackBar.open('Invalid file type. Please select a video file', 'Dismiss', { duration: 5000 });
      return;
    }
    
    // Clear previous video
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }
    
    // Store file and create preview URL
    this.videoFile = file;
    this.previewUrl = URL.createObjectURL(file);
    
    // Auto-populate title if not set and not in edit mode
    if (!this.isEditing && !this.uploadForm.get('title')?.value) {
      // Get filename without extension
      const title = file.name.replace(/\.[^/.]+$/, "").replace(/[-_]/g, ' ');
      this.uploadForm.get('title')?.setValue(title);
    }
  }
  
  private handleThumbnailFile(file: File): void {
    // Validate file type
    if (!file.type.includes('image/')) {
      this.error = 'Please select a valid image file for the thumbnail';
      this.snackBar.open('Invalid file type. Please select an image file', 'Dismiss', { duration: 5000 });
      return;
    }
    
    // Clear previous thumbnail
    if (this.thumbnailUrl) {
      URL.revokeObjectURL(this.thumbnailUrl);
    }
    
    // Store file and create preview URL
    this.thumbnailFile = file;
    this.thumbnailUrl = URL.createObjectURL(file);
  }
  
  addTag(event: MatChipInputEvent | any): void {
    // Handle both MatChipInputEvent and standard events
    const value = (event.value || (event.chipInput?.value) || '').trim();
    
    // Add tag if it doesn't exist already
    if (value && this.tags.length < 10 && !this.tags.includes(value)) {
      this.tags.push(value);
    }
    
    // Clear the input
    if (event.chipInput) {
      event.chipInput.clear();
    } else if (event.target) {
      // Handle standard DOM event
      (event.target as HTMLInputElement).value = '';
    }
  }
  
  removeTag(tag: string): void {
    const index = this.tags.indexOf(tag);
    if (index >= 0) {
      this.tags.splice(index, 1);
    }
  }
  
  convertToWebM(): void {
    if (!this.videoFile || !this.webWorker) return;
    
    // Start conversion
    this.isConverting = true;
    this.conversionProgress = 0;
    
    // Send the video to the web worker
    this.webWorker.postMessage({
      type: 'convert',
      video: this.videoFile
    });
  }
  
  private setupWebWorkerListeners(): void {
    if (!this.webWorker) return;
    
    this.webWorker.onmessage = (event) => {
      const { type, progress, video, message, error } = event.data;
      
      switch (type) {
        case 'progress':
          this.conversionProgress = progress;
          break;
          
        case 'complete':
          // Clean up previous video
          if (this.previewUrl) {
            URL.revokeObjectURL(this.previewUrl);
          }
          
          // Update with converted video
          this.videoFile = new File([video], 
            this.videoFile?.name.replace(/\.[^/.]+$/, "") + '.webm', 
            { type: 'video/webm' });
          this.previewUrl = URL.createObjectURL(video);
          
          this.isConverting = false;
          this.conversionProgress = 100;
          
          this.snackBar.open('Video successfully converted to WebM', 'Dismiss', { 
            duration: 5000 
          });
          break;
          
        case 'error':
          this.isConverting = false;
          this.error = message || 'An error occurred during conversion';
          this.snackBar.open('Video conversion failed', 'Dismiss', { duration: 5000 });
          break;
      }
    };
    
    this.webWorker.onerror = (error) => {
      this.isConverting = false;
      this.error = 'Web Worker error: ' + error.message;
      this.snackBar.open('Video conversion failed', 'Dismiss', { duration: 5000 });
    };
  }
  
  onSubmit(): void {
    // Validate form
    if (this.uploadForm.invalid) {
      this.snackBar.open('Please complete all required fields correctly', 'Dismiss', { duration: 5000 });
      return;
    }
    
    // In edit mode, we need different handling
    if (this.isEditing) {
      this.updateVideo();
    } else {
      // Check if files are selected
      if (!this.videoFile || !this.thumbnailFile) {
        this.snackBar.open('Please select both video and thumbnail files', 'Dismiss', { duration: 5000 });
        return;
      }
      
      this.uploadNewVideo();
    }
  }
  
  private uploadNewVideo(): void {
    if (!this.videoFile || !this.thumbnailFile) return;
    
    this.isLoading = true;
    
    const videoData: VideoUpload = {
      ...this.uploadForm.value,
      tags: this.tags
    };
    
    this.videoService.uploadVideo(videoData, this.videoFile, this.thumbnailFile)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (event) => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            const percentDone = Math.round(100 * event.loaded / event.total);
            this.conversionProgress = percentDone;
          } else if (event instanceof HttpResponse) {
            this.snackBar.open('Video uploaded successfully', 'Dismiss', { duration: 5000 });
            this.router.navigate(['/admin/videos']);
          }
        },
        error: (err) => {
          this.error = 'Upload failed: ' + (err.error?.message || err.statusText);
          this.snackBar.open('Failed to upload video', 'Dismiss', { duration: 5000 });
        }
      });
  }
  
  private updateVideo(): void {
    if (!this.videoId) return;
    
    this.isLoading = true;
    
    const videoData: VideoUpload = {
      ...this.uploadForm.value,
      tags: this.tags
    };
    
    // First update the video metadata
    this.videoService.updateVideo(this.videoId, videoData)
      .subscribe({
        next: (updatedVideo) => {
          // If there's a new thumbnail, update it
          if (this.thumbnailFile) {
            this.videoService.updateThumbnail(this.videoId!, this.thumbnailFile)
              .pipe(finalize(() => this.isLoading = false))
              .subscribe({
                next: () => this.handleSuccessfulUpdate(),
                error: (err) => this.handleUpdateError(err)
              });
          } else {
            this.isLoading = false;
            this.handleSuccessfulUpdate();
          }
        },
        error: (err) => {
          this.isLoading = false;
          this.handleUpdateError(err);
        }
      });
  }
  
  private handleSuccessfulUpdate(): void {
    this.snackBar.open('Video updated successfully', 'Dismiss', { duration: 5000 });
    this.router.navigate(['/admin/videos']);
  }
  
  private handleUpdateError(err: any): void {
    this.error = 'Update failed: ' + (err.error?.message || err.statusText);
    this.snackBar.open('Failed to update video', 'Dismiss', { duration: 5000 });
  }
  
  onCancel(): void {
    this.location.back();
  }
}
