import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  AiPlanItem,
  DailyPlannerResponse,
  TaskService
} from '../../services/task.service';

@Component({
  selector: 'app-planner',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './planner.html',
  styleUrl: './planner.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlannerComponent {

  private readonly taskService = inject(TaskService);
  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  planningDate = this.formatDate(
    new Date()
  );

  availableFrom = '09:00';

  availableUntil = '22:00';

  isLoading = false;

  errorMessage = '';

  successMessage = '';

  plannerResponse: DailyPlannerResponse | null = null;

  ngOnInit(): void {
    this.generatePlan();
  }

  generatePlan(): void {

    if (
      !this.planningDate ||
      !this.availableFrom ||
      !this.availableUntil
    ) {
      this.errorMessage =
        'Please provide a planning date and available time.';
      return;
    }

    if (
      this.availableFrom >= this.availableUntil
    ) {
      this.errorMessage =
        'Available start time must be before end time.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.changeDetectorRef.detectChanges();

    this.taskService
      .generateDailyPlan(
        this.planningDate,
        this.availableFrom,
        this.availableUntil
      )
      .subscribe({

        next: (response) => {

          this.plannerResponse =
            response;

          this.successMessage =
            'Your daily plan was generated successfully.';

          this.isLoading = false;

          this.changeDetectorRef.detectChanges();
        },

        error: (error) => {

          console.error(
            'Failed to generate daily plan:',
            error
          );

          this.isLoading = false;

          if (error?.status === 401) {

            this.errorMessage =
              'Your session has expired. Please log in again.';

          } else if (error?.status === 400) {

            this.errorMessage =
              error?.error?.message ??
              'The planning inputs are invalid.';

          } else if (error?.status === 409) {

            this.errorMessage =
              error?.error?.message ??
              'The planner could not create a valid plan.';

          } else {

            this.errorMessage =
              'Unable to generate your plan. Please try again.';
          }

          this.changeDetectorRef.detectChanges();
        }
      });
  }

  get planItems(): AiPlanItem[] {

    return (
      this.plannerResponse
        ?.plan
        ?.planData
        ?.items ?? []
    );
  }

  get totalPlannedMinutes(): number {

    return this.planItems.reduce(
      (total, item) =>
        total +
        this.calculateDuration(
          item.startAt,
          item.endAt
        ),
      0
    );
  }

  get availableMinutes(): number {

    if (
      !this.availableFrom ||
      !this.availableUntil
    ) {
      return 0;
    }

    const start =
      this.timeToMinutes(
        this.availableFrom
      );

    const end =
      this.timeToMinutes(
        this.availableUntil
      );

    return Math.max(
      0,
      end - start
    );
  }

  get utilizationPercentage(): number {

    if (
      this.availableMinutes <= 0
    ) {
      return 0;
    }

    return Math.min(
      100,
      Math.round(
        (
          this.totalPlannedMinutes /
          this.availableMinutes
        ) * 100
      )
    );
  }

  get planSummary(): string {

    return (
      this.plannerResponse
        ?.plan
        ?.summary ??
      'No plan generated yet.'
    );
  }

  formatTime(
    value: string
  ): string {

    if (!value) {
      return '';
    }

    const parts =
      value.split(':');

    if (parts.length < 2) {
      return value;
    }

    const hour =
      Number(parts[0]);

    const minute =
      parts[1];

    const period =
      hour >= 12
        ? 'PM'
        : 'AM';

    const displayHour =
      hour % 12 || 12;

    return `${displayHour}:${minute} ${period}`;
  }

  formatDuration(
    minutes: number
  ): string {

    if (minutes < 60) {
      return `${minutes} min`;
    }

    const hours =
      Math.floor(minutes / 60);

    const remaining =
      minutes % 60;

    if (remaining === 0) {
      return `${hours}h`;
    }

    return `${hours}h ${remaining}m`;
  }

  getTaskTimeRange(
    item: AiPlanItem
  ): string {

    return `${this.formatTime(item.startAt)} → ${this.formatTime(item.endAt)}`;
  }

  getTaskDuration(
    item: AiPlanItem
  ): string {

    return this.formatDuration(
      this.calculateDuration(
        item.startAt,
        item.endAt
      )
    );
  }

  getPriorityClass(
    item: AiPlanItem
  ): string {

    return 'priority-default';
  }

  private calculateDuration(
    startAt: string,
    endAt: string
  ): number {

    const start =
      this.timeToMinutes(
        startAt.substring(0, 5)
      );

    const end =
      this.timeToMinutes(
        endAt.substring(0, 5)
      );

    return Math.max(
      0,
      end - start
    );
  }

  private timeToMinutes(
    value: string
  ): number {

    const parts =
      value.split(':');

    return (
      Number(parts[0]) * 60 +
      Number(parts[1])
    );
  }

  private formatDate(
    date: Date
  ): string {

    const year =
      date.getFullYear();

    const month =
      String(
        date.getMonth() + 1
      ).padStart(2, '0');

    const day =
      String(
        date.getDate()
      ).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}