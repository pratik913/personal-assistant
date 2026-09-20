import {
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  finalize
} from 'rxjs';

import {
  PlannerService,
  AiPlanResponse,
  AiPlanItem
} from '../../services/planner.service';

@Component({
  selector: 'app-planner',
  imports: [FormsModule],
  templateUrl: './planner.html',
  styleUrl: './planner.scss'
})
export class Planner {

  private readonly plannerService =
    inject(PlannerService);

  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  planningDate =
    new Date().toISOString().split('T')[0];

  availableFrom = '09:00';

  availableUntil = '18:00';

  isGenerating = false;

  errorMessage = '';

  plan: AiPlanResponse | null = null;

  generatePlan(): void {

    this.errorMessage = '';
    this.plan = null;

    if (!this.planningDate) {
      this.errorMessage =
        'Please select a planning date.';
      return;
    }

    if (!this.availableFrom ||
        !this.availableUntil) {

      this.errorMessage =
        'Please select your available time.';
      return;
    }

    if (this.availableFrom >= this.availableUntil) {
      this.errorMessage =
        'Available time must have a valid range.';
      return;
    }

    this.isGenerating = true;

    this.plannerService
      .createPlan({
        planningDate: this.planningDate,
        availableFrom: this.availableFrom,
        availableUntil: this.availableUntil
      })
      .pipe(
        finalize(() => {
          this.isGenerating = false;
          this.changeDetectorRef.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          this.plan = response;

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'PLANNER API ERROR:',
            error
          );

          if (error.status === 401) {

            this.errorMessage =
              'Your session has expired. Please log in again.';

          } else if (error.status === 400) {

            this.errorMessage =
              error.error?.message ??
              'Please check your planning details.';

          } else {

            this.errorMessage =
              'Unable to generate your AI plan. Please try again.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  formatTime(timestamp: string): string {

    return new Intl.DateTimeFormat(
      'en-IN',
      {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
        timeZone: 'Asia/Kolkata'
      }
    ).format(
      new Date(timestamp)
    );
  }

  getDuration(item: AiPlanItem): string {

    const start =
      new Date(item.startAt).getTime();

    const end =
      new Date(item.endAt).getTime();

    const minutes =
      Math.round(
        (end - start) / 60000
      );

    const hours =
      Math.floor(minutes / 60);

    const remainingMinutes =
      minutes % 60;

    if (hours === 0) {
      return `${remainingMinutes} min`;
    }

    if (remainingMinutes === 0) {
      return `${hours} hr`;
    }

    return `${hours} hr ${remainingMinutes} min`;
  }
}