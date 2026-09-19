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

  isSubmitting = false;
  errorMessage = '';
  captureResult: CaptureResponse | null = null;

  selectType(type: CaptureType): void {
    this.captureType = type;
    this.errorMessage = '';
    this.captureResult = null;
  }

  useExample(example: string): void {
    this.captureType = 'TEXT';
    this.content = example;
    this.errorMessage = '';
    this.captureResult = null;
  }

  submitCapture(): void {

    this.errorMessage = '';
    this.captureResult = null;

    if (this.captureType === 'TEXT' && !this.content.trim()) {
      this.errorMessage = 'Please enter something to capture.';
      return;
    }

    if (this.captureType === 'URL' && !this.sourceUrl.trim()) {
      this.errorMessage = 'Please enter a URL.';
      return;
    }

    const request = {
      type: this.captureType,
      content: this.content.trim() || undefined,
      sourceUrl: this.sourceUrl.trim() || undefined
    };


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

          // Clear the input after successful capture.
          this.content = '';
          this.sourceUrl = '';

         

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {
          console.error('API ERROR:', error);

          if (error.status === 401) {
            this.errorMessage =
              'Your session has expired. Please log in again.';
          } else if (error.status === 400) {
            this.errorMessage =
              error.error?.message ??
              'Please check the capture details.';
          } else {
            this.errorMessage =
              'Something went wrong while analyzing your capture.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }
}