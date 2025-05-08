/**
 * Video Converter Web Worker
 * Handles client-side conversion of videos to WebM format for Bitzomax platform
 */

// Set up the self context for the web worker
self.onmessage = function(e) {
  const { type, video } = e.data;
  
  if (type === 'convert') {
    try {
      convertToWebM(video);
    } catch (error) {
      self.postMessage({
        type: 'error',
        message: error.message || 'Failed to convert video'
      });
    }
  }
};

/**
 * Converts a video file to WebM format using MediaRecorder API
 * @param {File} videoFile - The video file to convert
 */
function convertToWebM(videoFile) {
  // Create object URL for the video file
  const videoURL = URL.createObjectURL(videoFile);
  
  // Create a video element to load and process the video
  const videoElement = document.createElement('video');
  videoElement.src = videoURL;
  videoElement.muted = true;
  
  // Set up event handlers
  videoElement.onloadedmetadata = function() {
    // Report initial loading progress
    self.postMessage({
      type: 'progress',
      progress: 10
    });
    
    // Create canvas with video dimensions
    const canvas = new OffscreenCanvas(videoElement.videoWidth, videoElement.videoHeight);
    const ctx = canvas.getContext('2d');
    
    // Set up MediaRecorder with WebM format
    const stream = canvas.captureStream(30); // 30fps
    const mediaRecorder = new MediaRecorder(stream, {
      mimeType: 'video/webm',
      videoBitsPerSecond: 2500000 // 2.5 Mbps - good balance of quality and size
    });
    
    const chunks = [];
    
    // Handle recording data
    mediaRecorder.ondataavailable = function(e) {
      if (e.data.size > 0) {
        chunks.push(e.data);
      }
    };
    
    // Handle recording completion
    mediaRecorder.onstop = function() {
      // Release resources
      URL.revokeObjectURL(videoURL);
      
      // Create the final WebM video blob
      const webmBlob = new Blob(chunks, { type: 'video/webm' });
      
      // Send the converted video back to the main thread
      self.postMessage({
        type: 'complete',
        video: webmBlob
      });
    };
    
    // Handle recording errors
    mediaRecorder.onerror = function(event) {
      URL.revokeObjectURL(videoURL);
      self.postMessage({
        type: 'error',
        message: 'MediaRecorder error: ' + event.error
      });
    };
    
    // Start recording
    mediaRecorder.start(100); // 100ms chunks
    
    // Report start of conversion
    self.postMessage({
      type: 'progress',
      progress: 20
    });
    
    // Process the video frames
    let currentTime = 0;
    const duration = videoElement.duration;
    const frameInterval = 1000 / 30; // 30fps = frame every ~33ms
    
    function processNextFrame() {
      if (currentTime <= duration) {
        // Update video playback position
        videoElement.currentTime = currentTime;
        
        // Draw the current frame to canvas
        ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
        
        // Calculate and report progress
        const progress = Math.min(20 + Math.floor(80 * currentTime / duration), 99);
        self.postMessage({
          type: 'progress',
          progress: progress
        });
        
        // Advance to next frame
        currentTime += frameInterval / 1000;
        
        // Process next frame after a short delay to allow video seeking
        setTimeout(processNextFrame, 10);
      } else {
        // Finished processing frames, stop recording
        mediaRecorder.stop();
      }
    }
    
    // Start frame processing
    videoElement.onseeked = function() {
      // Draw frame on canvas
      ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
    };
    
    // Begin processing
    processNextFrame();
  };
  
  // Handle loading errors
  videoElement.onerror = function(error) {
    URL.revokeObjectURL(videoURL);
    self.postMessage({
      type: 'error',
      message: 'Failed to load video: ' + (error.message || 'Unknown error')
    });
  };
  
  // Report initial status
  self.postMessage({
    type: 'progress',
    progress: 5
  });
  
  // Start loading the video
  videoElement.load();
}