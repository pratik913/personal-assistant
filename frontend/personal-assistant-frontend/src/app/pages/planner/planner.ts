import { CommonModule } from '@angular/common';

import {
  ChangeDetectorRef,
  Component,
  NgZone,
  inject
} from '@angular/core';

import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';

import { FormsModule } from '@angular/forms';

import { RouterLink } from '@angular/router';

import { firstValueFrom } from 'rxjs';

interface PlannerItem {
  taskId: string;
  taskTitle: string;
  startAt: string;
  endAt: string;
  reason: string;
}

interface PlannerData {
  planningDate: string;
  items: PlannerItem[];

  // The backend returns planningDate + items.
  // These are populated client-side so the existing
  // template can continue displaying the selected
  // planning window.
  availableFrom?: string;
  availableUntil?: string;
}

interface CreateDailyPlanRequest {
  planningDate: string;
  availableFrom: string;
  availableUntil: string;
}

interface ApplyDailyPlanRequest {
  items: PlannerItem[];
}

interface ScheduleEntryResponse {
  id: string;
  taskId: string;
  startAt: string;
  endAt: string;
}

@Component({
  selector: 'app-planner',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './planner.html',
  styleUrl: './planner.scss'
})
export class Planner {

  private readonly http = inject(HttpClient);

  private readonly cdr =
    inject(ChangeDetectorRef);

  private readonly ngZone =
    inject(NgZone);

  private readonly plannerUrl =
    'http://localhost:8080/api/planner/daily';

  private readonly applyPlannerUrl =
    'http://localhost:8080/api/planner/daily/apply';


  // =========================================================
  // FORM
  // =========================================================

  readonly today = this.getTodayDate();

  planningDate = this.today;

  availableFrom = '09:00';

  availableUntil = '18:00';


  // =========================================================
  // STATE
  // =========================================================

  plan: PlannerData | null = null;

  isGenerating = false;

  isApplying = false;

  errorMessage = '';

  applyErrorMessage = '';

  applySuccessMessage = '';

  appliedScheduleEntries:
    ScheduleEntryResponse[] = [];


  // =========================================================
  // DERIVED DATA
  // =========================================================

  get planItems(): PlannerItem[] {

    const items =
      this.plan?.items ?? [];

    return [...items].sort(
      (first, second) => {
        return (
          new Date(first.startAt).getTime() -
          new Date(second.startAt).getTime()
        );
      }
    );
  }


  get planSummary(): string {

    if (!this.plan) {
      return '';
    }

    if (this.planItems.length === 0) {
      return 'No tasks fit within the selected planning window.';
    }

    return `${this.planItems.length} task${
      this.planItems.length === 1 ? '' : 's'
    } selected using your tasks, schedule and planning history.`;
  }


  get hasPlan(): boolean {
    return this.plan !== null;
  }


  get hasPlanItems(): boolean {
    return this.planItems.length > 0;
  }


  get isPlanApplied(): boolean {
    return (
      this.appliedScheduleEntries.length > 0
    );
  }


  // =========================================================
  // GENERATE PLAN
  // =========================================================

  async generatePlan(): Promise<void> {

    if (
      this.isGenerating ||
      this.isApplying
    ) {
      return;
    }


    // -------------------------------------------------------
    // Reset previous state
    // -------------------------------------------------------

    this.errorMessage = '';

    this.applyErrorMessage = '';

    this.applySuccessMessage = '';

    this.appliedScheduleEntries = [];


    // -------------------------------------------------------
    // Validate date
    // -------------------------------------------------------

    if (!this.planningDate) {

      this.errorMessage =
        'Please select a planning date.';

      this.refreshView();

      return;
    }


    if (this.planningDate < this.today) {

      this.errorMessage =
        'You cannot create a plan for a past date.';

      this.refreshView();

      return;
    }


    // -------------------------------------------------------
    // Validate time
    // -------------------------------------------------------

    if (
      !this.availableFrom ||
      !this.availableUntil
    ) {

      this.errorMessage =
        'Please select your available time window.';

      this.refreshView();

      return;
    }


    if (
      this.availableFrom >=
      this.availableUntil
    ) {

      this.errorMessage =
        'Available-from time must be before available-until time.';

      this.refreshView();

      return;
    }


    // -------------------------------------------------------
    // Start loading state
    // -------------------------------------------------------

    this.isGenerating = true;

    this.plan = null;

    this.refreshView();


    console.log(
      '[MindMate Planner] Generating plan...'
    );

    console.log(
      '[MindMate Planner] Request:',
      {
        planningDate: this.planningDate,
        availableFrom: this.availableFrom,
        availableUntil: this.availableUntil
      }
    );


    try {

      // -----------------------------------------------------
      // REQUEST BODY
      // -----------------------------------------------------

      const request:
        CreateDailyPlanRequest = {

        planningDate:
          this.planningDate,

        availableFrom:
          this.availableFrom,

        availableUntil:
          this.availableUntil
      };


      // -----------------------------------------------------
      // HTTP REQUEST
      // -----------------------------------------------------

      const response =
        await firstValueFrom(
          this.http.post<PlannerData>(
            this.plannerUrl,
            request
          )
        );


      // -----------------------------------------------------
      // RESPONSE DEBUGGING
      // -----------------------------------------------------

      console.log(
        '[MindMate Planner] Response received:',
        response
      );

      console.log(
        '[MindMate Planner] Items:',
        response?.items
      );


      // -----------------------------------------------------
      // ENTER ANGULAR ZONE
      // -----------------------------------------------------

      this.ngZone.run(() => {

        this.plan = {
          ...response,
          availableFrom:
            this.availableFrom,
          availableUntil:
            this.availableUntil
        };

        this.isGenerating = false;

        this.errorMessage = '';

        this.refreshView();

      });


    } catch (error) {

      console.error(
        '[MindMate Planner] Request failed:',
        error
      );


      this.ngZone.run(() => {

        this.isGenerating = false;

        this.plan = null;

        this.errorMessage =
          this.getErrorMessage(
            error
          );

        this.refreshView();

      });


    } finally {

      // -----------------------------------------------------
      // FINAL SAFETY NET
      // -----------------------------------------------------

      this.ngZone.run(() => {

        if (!this.plan) {
          this.isGenerating = false;
        }

        this.refreshView();

      });

    }
  }


  // =========================================================
  // APPLY PLAN TO SCHEDULE
  // =========================================================

  async applyPlan(): Promise<void> {

    if (
      this.isApplying ||
      this.isGenerating ||
      !this.plan ||
      this.planItems.length === 0
    ) {
      return;
    }


    // -------------------------------------------------------
    // Reset apply state
    // -------------------------------------------------------

    this.applyErrorMessage = '';

    this.applySuccessMessage = '';

    this.appliedScheduleEntries = [];


    // -------------------------------------------------------
    // Start applying state
    // -------------------------------------------------------

    this.isApplying = true;

    this.refreshView();


    console.log(
      '[MindMate Planner] Applying plan...'
    );

    console.log(
      '[MindMate Planner] Apply request:',
      {
        planningDate:
          this.planningDate,

        availableFrom:
          this.availableFrom,

        availableUntil:
          this.availableUntil,

        items:
          this.planItems
      }
    );


    try {

      // -----------------------------------------------------
      // REQUEST BODY
      // -----------------------------------------------------

      const request:
        ApplyDailyPlanRequest = {
        items:
          this.planItems
      };


      // -----------------------------------------------------
      // APPLY REQUEST
      // -----------------------------------------------------

      const response =
        await firstValueFrom(
          this.http.post<ScheduleEntryResponse[]>(
            this.applyPlannerUrl,
            request
          )
        );


      console.log(
        '[MindMate Planner] Plan applied:',
        response
      );


      // -----------------------------------------------------
      // ENTER ANGULAR ZONE
      // -----------------------------------------------------

      this.ngZone.run(() => {

        this.appliedScheduleEntries =
          response ?? [];

        this.isApplying = false;

        this.applyErrorMessage = '';

        this.applySuccessMessage =
          'Your plan has been added to your schedule.';

        this.refreshView();

      });


    } catch (error) {

      console.error(
        '[MindMate Planner] Failed to apply plan:',
        error
      );


      this.ngZone.run(() => {

        this.isApplying = false;

        this.appliedScheduleEntries = [];

        this.applySuccessMessage = '';

        this.applyErrorMessage =
          this.getErrorMessage(
            error,
            'Unable to apply your plan to the schedule.'
          );

        this.refreshView();

      });


    } finally {

      this.ngZone.run(() => {

        this.isApplying = false;

        this.refreshView();

      });

    }
  }


  // =========================================================
  // TIME FORMATTING
  // =========================================================

  formatPlanTime(
    value: string
  ): string {

    if (!value) {
      return '--:--';
    }


    const date =
      new Date(value);


    if (
      Number.isNaN(
        date.getTime()
      )
    ) {

      return '--:--';
    }


    return new Intl.DateTimeFormat(
      undefined,
      {
        hour: 'numeric',
        minute: '2-digit'
      }
    ).format(date);
  }


  // =========================================================
  // DATE
  // =========================================================

  private getTodayDate(): string {

    const date =
      new Date();


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


  // =========================================================
  // TRACKING
  // =========================================================

  trackByTask(
    _index: number,
    item: PlannerItem
  ): string {

    return item.taskId;
  }


  // =========================================================
  // ERROR HANDLING
  // =========================================================

  private getErrorMessage(
    error: unknown,
    fallbackMessage =
      'Unable to generate your daily plan.'
  ): string {

    if (
      error instanceof HttpErrorResponse
    ) {

      if (
        error.error &&
        typeof error.error === 'object' &&
        typeof error.error.message === 'string'
      ) {

        return error.error.message;
      }


      if (
        typeof error.error === 'string' &&
        error.error.trim()
      ) {

        return error.error;
      }


      if (
        error.message &&
        error.message.trim()
      ) {

        return error.message;
      }
    }


    if (
      error instanceof Error &&
      error.message
    ) {

      return error.message;
    }


    return fallbackMessage;
  }


  // =========================================================
  // FORCE VIEW UPDATE
  // =========================================================

  refreshView(): void {

    this.cdr.detectChanges();

  }

}