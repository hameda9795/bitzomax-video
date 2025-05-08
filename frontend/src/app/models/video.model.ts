export interface Video {
  id: number;
  title: string;
  description: string;
  videoUrl: string;
  thumbnailUrl: string;
  premium: boolean;
  poemText?: string;
  duration: number;
  viewCount: number;
  likesCount: number;
  uploadDate: Date;
  tags: string[];
  seoTitle?: string;
  seoDescription?: string;
  seoKeywords?: string;
  uploader: {
    id: number;
    username: string;
    profilePicture?: string;
  };
}

export interface VideoUpload {
  title: string;
  description: string;
  premium: boolean;
  poemText?: string;
  tags: string[];
  seoTitle?: string;
  seoDescription?: string;
  seoKeywords?: string;
}

export interface PagedVideos {
  content: Video[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface VideoStatistics {
  totalVideos: number;
  totalPremiumVideos: number;
  totalViews: number;
  totalLikes: number;
  mostViewedVideo: Video;
  mostLikedVideo: Video;
  recentUploads: Video[];
  uploadsByMonth: {
    month: string;
    count: number;
  }[];
  viewsByMonth: {
    month: string;
    count: number;
  }[];
}

export interface VideoComment {
  id: number;
  videoId: number;
  text: string;
  createdDate: Date;
  user: {
    id: number;
    username: string;
    profilePicture?: string;
  };
}