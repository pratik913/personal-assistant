import {
  ChangeDetectorRef,
  Component,
  inject,
  OnDestroy
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  finalize
} from 'rxjs';

import {
  CaptureService,
  CaptureType,
  CaptureResponse
} from '../../services/capture.service';

@Component({
  selector: 'app-capture',

  imports: [
    FormsModule
  ],

  templateUrl: './capture.html',

  styleUrl: './capture.scss'
})
export class Capture
  implements OnDestroy {

  private readonly captureService =
    inject(CaptureService);

  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  captureType: CaptureType =
    'TEXT';

  content = '';

  sourceUrl = '';

  isInstagramUrl = false;

  selectedImage: File | null =
    null;

  imagePreviewUrl:
    string | null =
    null;

  voiceFile: File | null =
    null;

  voicePreviewUrl:
    string | null =
    null;

  isRecording =
    false;

  recordingSeconds =
    0;

  isSubmitting =
    false;

  errorMessage =
    '';

  captureResult:
    CaptureResponse | null =
    null;

  private mediaRecorder:
    MediaRecorder | null =
    null;

  private mediaStream:
    MediaStream | null =
    null;

  private voiceChunks:
    Blob[] = [];

  private recordingTimer:
    ReturnType<typeof setInterval> | null =
    null;

  // ============================================================
  // TYPE
  // ============================================================

  selectType(
    type: CaptureType
  ): void {

    if (this.isRecording) {
      this.stopRecording();
    }

    this.captureType =
      type;

    this.errorMessage =
      '';

    this.captureResult =
      null;

    this.isInstagramUrl =
      false;

    if (
      type !== 'IMAGE'
    ) {

      this.clearImage();
    }

    if (
      type !== 'VOICE'
    ) {

      this.clearVoice();
    }
  }

  useExample(
    example: string
  ): void {

    this.selectType(
      'TEXT'
    );

    this.content =
      example;

    this.sourceUrl =
      '';

    this.isInstagramUrl =
      false;

    this.errorMessage =
      '';

    this.captureResult =
      null;
  }

  onUrlChanged(): void {

    this.isInstagramUrl =
      this.detectInstagramUrl(
        this.sourceUrl
      );
  }

  private detectInstagramUrl(
    url: string
  ): boolean {

    if (!url.trim()) {
      return false;
    }

    const normalized =
      url.trim().toLowerCase();

    return normalized.includes(
      'instagram.com/reel/'
    ) || normalized.includes(
      'instagram.com/reels/'
    );
  }

  // ============================================================
  // IMAGE
  // ============================================================

  onImageSelected(
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    const file =
      input.files?.[0];

    if (!file) {
      return;
    }

    this.setSelectedImage(
      file
    );
  }

  onImageDrop(
    event: DragEvent
  ): void {

    event.preventDefault();

    const file =
      event.dataTransfer
        ?.files?.[0];

    if (!file) {
      return;
    }

    this.setSelectedImage(
      file
    );
  }

  onDragOver(
    event: DragEvent
  ): void {

    event.preventDefault();
  }

  removeSelectedImage(): void {

    this.clearImage();
  }

  private setSelectedImage(
    file: File
  ): void {

    this.errorMessage =
      '';

    this.captureResult =
      null;

    const supportedTypes = [
      'image/jpeg',
      'image/png',
      'image/webp'
    ];

    if (
      !supportedTypes.includes(
        file.type
      )
    ) {

      this.errorMessage =
        'Please select a JPG, PNG, or WebP image.';

      return;
    }

    if (
      file.size >
      5 * 1024 * 1024
    ) {

      this.errorMessage =
        'Screenshot must not exceed 5 MB.';

      return;
    }

    this.clearImage();

    this.selectedImage =
      file;

    this.imagePreviewUrl =
      URL.createObjectURL(
        file
      );
  }

  private clearImage(): void {

    if (
      this.imagePreviewUrl
    ) {

      URL.revokeObjectURL(
        this.imagePreviewUrl
      );
    }

    this.selectedImage =
      null;

    this.imagePreviewUrl =
      null;
  }

  // ============================================================
  // VOICE RECORDING
  // ============================================================

  async startRecording(): Promise<void> {

    this.errorMessage =
      '';

    this.captureResult =
      null;

    if (
      !navigator.mediaDevices
        ?.getUserMedia
    ) {

      this.errorMessage =
        'Voice recording is not supported by this browser.';

      return;
    }

    if (
      typeof MediaRecorder ===
      'undefined'
    ) {

      this.errorMessage =
        'Voice recording is not supported by this browser.';

      return;
    }

    try {

      this.clearVoice();

      this.mediaStream =
        await navigator
          .mediaDevices
          .getUserMedia({
            audio: true
          });

      const mimeType =
        this.getSupportedRecordingMimeType();

      this.mediaRecorder =
        mimeType
          ? new MediaRecorder(
              this.mediaStream,
              {
                mimeType
              }
            )
          : new MediaRecorder(
              this.mediaStream
            );

      this.voiceChunks =
        [];

      this.recordingSeconds =
        0;

      this.isRecording =
        true;

      this.mediaRecorder
        .ondataavailable =
        (
          event: BlobEvent
        ) => {

          if (
            event.data.size >
            0
          ) {

            this.voiceChunks.push(
              event.data
            );
          }
        };

      this.mediaRecorder
        .onstop =
        () => {

          const type =
            this.mediaRecorder
              ?.mimeType
              ||
              this.voiceChunks[0]
                ?.type
              ||
              'audio/webm';

          const blob =
            new Blob(
              this.voiceChunks,
              {
                type
              }
            );

          const extension =
            this.getAudioExtension(
              type
            );

          this.voiceFile =
            new File(
              [blob],
              `mindmate-voice-${Date.now()}.${extension}`,
              {
                type
              }
            );

          this.voicePreviewUrl =
            URL.createObjectURL(
              blob
            );

          this.cleanupRecordingResources();

          this.changeDetectorRef
            .detectChanges();
        };

      this.mediaRecorder.start();

      this.startRecordingTimer();

      this.changeDetectorRef
        .detectChanges();

    } catch (error) {

      console.error(
        'VOICE RECORDING ERROR:',
        error
      );

      this.cleanupRecordingResources();

      this.errorMessage =
        'Microphone access was not available. Please allow microphone access and try again.';

      this.changeDetectorRef
        .detectChanges();
    }
  }

  stopRecording(): void {

    if (
      !this.mediaRecorder
        ||
      this.mediaRecorder.state ===
      'inactive'
    ) {

      this.cleanupRecordingResources();

      return;
    }

    this.mediaRecorder.stop();

    this.stopRecordingTimer();

    this.isRecording =
      false;

    this.changeDetectorRef
      .detectChanges();
  }

  clearVoiceRecording(): void {

    if (
      this.isRecording
    ) {

      this.stopRecording();
    }

    this.clearVoice();

    this.errorMessage =
      '';
  }

  private clearVoice(): void {

    if (
      this.voicePreviewUrl
    ) {

      URL.revokeObjectURL(
        this.voicePreviewUrl
      );
    }

    this.voiceFile =
      null;

    this.voicePreviewUrl =
      null;

    this.voiceChunks =
      [];

    this.recordingSeconds =
      0;
  }

  private getSupportedRecordingMimeType():
    string | null {

    const candidates = [

      'audio/webm;codecs=opus',

      'audio/webm',

      'audio/mp4',

      'audio/ogg;codecs=opus'

    ];

    for (
      const candidate
      of candidates
    ) {

      if (
        MediaRecorder.isTypeSupported(
          candidate
        )
      ) {

        return candidate;
      }
    }

    return null;
  }

  private getAudioExtension(
    mimeType: string
  ): string {

    const normalized =
      mimeType.toLowerCase();

    if (
      normalized.includes(
        'mp4'
      )
    ) {
      return 'mp4';
    }

    if (
      normalized.includes(
        'ogg'
      )
    ) {
      return 'ogg';
    }

    if (
      normalized.includes(
        'mpeg'
      )
    ) {
      return 'mp3';
    }

    if (
      normalized.includes(
        'wav'
      )
    ) {
      return 'wav';
    }

    return 'webm';
  }

  private startRecordingTimer(): void {

    this.stopRecordingTimer();

    this.recordingTimer =
      setInterval(() => {

        this.recordingSeconds +=
          1;

        this.changeDetectorRef
          .detectChanges();

      }, 1000);
  }

  private stopRecordingTimer(): void {

    if (
      this.recordingTimer
    ) {

      clearInterval(
        this.recordingTimer
      );

      this.recordingTimer =
        null;
    }
  }

  private cleanupRecordingResources(): void {

    this.stopRecordingTimer();

    if (
      this.mediaStream
    ) {

      this.mediaStream
        .getTracks()
        .forEach(
          track =>
            track.stop()
        );
    }

    this.mediaStream =
      null;

    this.mediaRecorder =
      null;

    this.isRecording =
      false;
  }

  // ============================================================
  // SUBMIT
  // ============================================================

  submitCapture(): void {

    this.errorMessage =
      '';

    this.captureResult =
      null;

    if (
      this.captureType ===
      'TEXT'
    ) {

      this.submitTextCapture();

      return;
    }

    if (
      this.captureType ===
      'URL'
    ) {

      this.submitUrlCapture();

      return;
    }

    if (
      this.captureType ===
      'IMAGE'
    ) {

      this.submitImageCapture();

      return;
    }

    if (
      this.captureType ===
      'VOICE'
    ) {

      this.submitVoiceCapture();
    }
  }

  private submitTextCapture(): void {

    if (
      !this.content.trim()
    ) {

      this.errorMessage =
        'Please enter something to capture.';

      return;
    }

    this.submitStandardCapture({

      type: 'TEXT',

      content:
        this.content.trim()
    });
  }

  private submitUrlCapture(): void {

    const url =
      this.sourceUrl.trim();

    if (!url) {

      this.errorMessage =
        'Please enter a URL.';

      return;
    }

    try {

      const parsedUrl =
        new URL(url);

      if (
        parsedUrl.protocol !== 'http:'
        &&
        parsedUrl.protocol !== 'https:'
      ) {

        this.errorMessage =
          'Please enter a valid HTTP or HTTPS URL.';

        return;
      }

    } catch {

      this.errorMessage =
        'Please enter a valid URL.';

      return;
    }

    this.submitStandardCapture({

      type: 'URL',

      content:
        this.content.trim()
        || undefined,

      sourceUrl:
        url
    });
  }

  private submitStandardCapture(
    request: {
      type:
        | 'TEXT'
        | 'URL';

      content?: string;

      sourceUrl?: string;
    }
  ): void {

    this.isSubmitting =
      true;

    this.changeDetectorRef
      .detectChanges();

    this.captureService
      .createCapture(
        request
      )
      .pipe(

        finalize(() => {

          this.isSubmitting =
            false;

          this.changeDetectorRef
            .detectChanges();
        })

      )
      .subscribe({

        next: (
          response
        ) => {

          this.captureResult =
            response;

          this.content =
            '';

          this.sourceUrl =
            '';

          this.isInstagramUrl =
            false;

          this.changeDetectorRef
            .detectChanges();
        },

        error: (
          error
        ) => {

          this.handleError(
            error
          );

          this.changeDetectorRef
            .detectChanges();
        }
      });
  }

  private submitImageCapture(): void {

    if (
      !this.selectedImage
    ) {

      this.errorMessage =
        'Please select a screenshot first.';

      return;
    }

    this.isSubmitting =
      true;

    this.changeDetectorRef
      .detectChanges();

    this.captureService
      .createImageCapture(
        this.selectedImage,
        this.content
      )
      .pipe(

        finalize(() => {

          this.isSubmitting =
            false;

          this.changeDetectorRef
            .detectChanges();
        })

      )
      .subscribe({

        next: (
          response
        ) => {

          this.captureResult =
            response;

          this.content =
            '';

          this.clearImage();

          this.changeDetectorRef
            .detectChanges();
        },

        error: (
          error
        ) => {

          this.handleError(
            error
          );

          this.changeDetectorRef
            .detectChanges();
        }
      });
  }

  private submitVoiceCapture(): void {

    if (
      this.isRecording
    ) {

      this.errorMessage =
        'Stop the recording before uploading it.';

      return;
    }

    if (
      !this.voiceFile
    ) {

      this.errorMessage =
        'Please record a voice note first.';

      return;
    }

    this.isSubmitting =
      true;

    this.changeDetectorRef
      .detectChanges();

    this.captureService
      .createVoiceCapture(
        this.voiceFile,
        this.content
      )
      .pipe(

        finalize(() => {

          this.isSubmitting =
            false;

          this.changeDetectorRef
            .detectChanges();
        })

      )
      .subscribe({

        next: (
          response
        ) => {

          this.captureResult =
            response;

          this.content =
            '';

          this.clearVoice();

          this.changeDetectorRef
            .detectChanges();
        },

        error: (
          error
        ) => {

          this.handleError(
            error
          );

          this.changeDetectorRef
            .detectChanges();
        }
      });
  }

  // ============================================================
  // ERROR HANDLING
  // ============================================================

private handleError(error: any): void {

  console.error(
    '========== CAPTURE API ERROR =========='
  );

  console.error(
    'Status:',
    error?.status
  );

  console.error(
    'Status text:',
    error?.statusText
  );

  console.error(
    'URL:',
    error?.url
  );

  console.error(
    'Error body:',
    error?.error
  );

  console.error(
    'Full error:',
    error
  );

  console.error(
    '======================================='
  );

  if (error?.status === 401) {

    const backendMessage =
      error?.error?.message;

    this.errorMessage =
      backendMessage
      ??
      'Screenshot request was rejected by the backend (401).';

    return;
  }

  if (error?.status === 400) {

    this.errorMessage =
      error?.error?.message
      ??
      'Please check the screenshot details.';

    return;
  }

  if (error?.status === 403) {

    this.errorMessage =
      'The backend rejected the screenshot request (403).';

    return;
  }

  if (error?.status === 413) {

    this.errorMessage =
      'Screenshot is too large. Maximum size is 5 MB.';

    return;
  }

  this.errorMessage =
    `Screenshot upload failed (${error?.status ?? 'unknown'}).`;
}

  // ============================================================
  // UI HELPERS
  // ============================================================

  formatRecordingTime(
    seconds: number
  ): string {

    const minutes =
      Math.floor(
        seconds / 60
      )
        .toString()
        .padStart(
          2,
          '0'
        );

    const remainingSeconds =
      (
        seconds % 60
      )
        .toString()
        .padStart(
          2,
          '0'
        );

    return `${minutes}:${remainingSeconds}`;
  }

  getResultTitle(): string {

    if (
      !this.captureResult
    ) {

      return '';
    }

    if (
      this.captureResult.type ===
      'IMAGE'
    ) {

      return 'Your screenshot was analyzed';
    }

    if (
      this.captureResult.type ===
      'VOICE'
    ) {

      return 'Your voice note was transcribed and analyzed';
    }

    if (
      this.captureResult.type ===
      'URL'
    ) {

      return 'Your link was analyzed';
    }

    return 'Your thought was analyzed';
  }

  getSubmitLabel(): string {

    if (
      this.captureType ===
      'IMAGE'
    ) {

      return 'Analyze Screenshot';
    }

    if (
      this.captureType ===
      'VOICE'
    ) {

      return 'Analyze Voice';
    }

    return 'Analyze with AI';
  }

  // ============================================================
  // CLEANUP
  // ============================================================

  ngOnDestroy(): void {

    if (
      this.isRecording
    ) {

      this.stopRecording();
    }

    this.cleanupRecordingResources();

    this.clearImage();

    this.clearVoice();
  }
}