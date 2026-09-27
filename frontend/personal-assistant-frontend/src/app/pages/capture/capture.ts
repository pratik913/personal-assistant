import {
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  CaptureService,
  CaptureType,
  CaptureResponse
} from '../../services/capture.service';

@Component({
  selector: 'app-capture',
  imports: [FormsModule],
  templateUrl: './capture.html',
  styleUrl: './capture.scss'
})
export class Capture {

  private readonly captureService = inject(CaptureService);
  private readonly changeDetectorRef = inject(ChangeDetectorRef);

  captureType: CaptureType = 'TEXT';

  content = '';
  sourceUrl = '';

  selectedImage: File | null = null;
  imagePreviewUrl: string | null = null;

  isSubmitting = false;
  errorMessage = '';
  captureResult: CaptureResponse | null = null;

  selectType(type: CaptureType): void {

    this.captureType = type;

    this.errorMessage = '';
    this.captureResult = null;

    if (type !== 'IMAGE') {
      this.clearImage();
    }
  }

  useExample(example: string): void {

    this.captureType = 'TEXT';

    this.content = example;

    this.sourceUrl = '';

    this.errorMessage = '';
    this.captureResult = null;

    this.clearImage();
  }

  onImageSelected(event: Event): void {

    const input = event.target as HTMLInputElement;

    const file = input.files?.[0];

    if (!file) {
      return;
    }

    this.setSelectedImage(file);
  }

  onImageDrop(event: DragEvent): void {

    event.preventDefault();

    const file = event.dataTransfer?.files?.[0];

    if (!file) {
      return;
    }

    this.setSelectedImage(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  removeSelectedImage(): void {
    this.clearImage();
  }

  submitCapture(): void {

    this.errorMessage = '';
    this.captureResult = null;

    if (this.captureType === 'TEXT') {
      this.submitTextCapture();
      return;
    }

    if (this.captureType === 'URL') {
      this.submitUrlCapture();
      return;
    }

    if (this.captureType === 'IMAGE') {
      this.submitImageCapture();
      return;
    }

    this.errorMessage =
      'Voice capture will be available in the next step.';
  }

  private submitTextCapture(): void {

    if (!this.content.trim()) {

      this.errorMessage =
        'Please enter something to capture.';

      return;
    }

    this.submitStandardCapture({
      type: 'TEXT',
      content: this.content.trim()
    });
  }

  private submitUrlCapture(): void {

    if (!this.sourceUrl.trim()) {

      this.errorMessage =
        'Please enter a URL.';

      return;
    }

    this.submitStandardCapture({
      type: 'URL',
      content: this.content.trim() || undefined,
      sourceUrl: this.sourceUrl.trim()
    });
  }

  private submitStandardCapture(request: {
    type: 'TEXT' | 'URL';
    content?: string;
    sourceUrl?: string;
  }): void {

    this.isSubmitting = true;

    this.changeDetectorRef.detectChanges();

    this.captureService
      .createCapture(request)
      .pipe(
        finalize(() => {

          this.isSubmitting = false;

          this.changeDetectorRef.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          this.captureResult = response;

          this.content = '';
          this.sourceUrl = '';

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          this.handleError(error);

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private submitImageCapture(): void {

    if (!this.selectedImage) {

      this.errorMessage =
        'Please select a screenshot first.';

      return;
    }

    this.isSubmitting = true;

    this.changeDetectorRef.detectChanges();

    this.captureService
      .createImageCapture(
        this.selectedImage,
        this.content
      )
      .pipe(
        finalize(() => {

          this.isSubmitting = false;

          this.changeDetectorRef.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          this.captureResult = response;

          this.content = '';

          this.clearImage();

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          this.handleError(error);

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  private setSelectedImage(file: File): void {

    this.errorMessage = '';
    this.captureResult = null;

    const supportedTypes = [
      'image/jpeg',
      'image/png',
      'image/webp'
    ];

    if (!supportedTypes.includes(file.type)) {

      this.errorMessage =
        'Please select a JPG, PNG, or WebP image.';

      return;
    }

    if (file.size > 5 * 1024 * 1024) {

      this.errorMessage =
        'Screenshot must not exceed 5 MB.';

      return;
    }

    this.clearImage();

    this.selectedImage = file;

    this.imagePreviewUrl =
      URL.createObjectURL(file);
  }

  private clearImage(): void {

    if (this.imagePreviewUrl) {
      URL.revokeObjectURL(
        this.imagePreviewUrl
      );
    }

    this.selectedImage = null;
    this.imagePreviewUrl = null;
  }

  private handleError(error: any): void {

    console.error(
      'CAPTURE API ERROR:',
      error
    );

    if (error.status === 401) {

      this.errorMessage =
        'Your session has expired. Please log in again.';

      return;
    }

    if (error.status === 400) {

      this.errorMessage =
        error.error?.message ??
        'Please check the capture details.';

      return;
    }

    this.errorMessage =
      'Something went wrong while analyzing your capture.';
  }
}