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
}

interface AiPlanResponse {
  id: string;
  summary: string;
  plan: PlannerData;
  createdAt: string;
  updatedAt: string;
}

interface DailyPlannerResponse {
  planningDate: string;
  availableFrom: string;
  availableUntil: string;
  plan: AiPlanResponse;
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

  private readonly cdr = inject(ChangeDetectorRef);

  private readonly ngZone = inject(NgZone);

  private readonly plannerUrl =
    'http://localhost:8080/api/daily-planner/generate';


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

  plan: DailyPlannerResponse | null = null;

  isGenerating = false;

  errorMessage = '';


  // =========================================================
  // DERIVED DATA
  // =========================================================

  get planItems(): PlannerItem[] {

    const items =
      this.plan?.plan?.plan?.items ?? [];

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

    return this.plan?.plan?.summary ?? '';
  }


  get hasPlan(): boolean {

    return this.plan !== null;
  }


  // =========================================================
  // GENERATE PLAN
  // =========================================================

  async generatePlan(): Promise<void> {

    if (this.isGenerating) {
      return;
    }


    // -------------------------------------------------------
    // Reset previous errors
    // -------------------------------------------------------

    this.errorMessage = '';


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
      // HTTP REQUEST
      // -----------------------------------------------------

      const response =
        await firstValueFrom(
          this.http.post<DailyPlannerResponse>(
            this.plannerUrl,
            null,
            {
              params: {
                planningDate:
                  this.planningDate,

                availableFrom:
                  this.availableFrom,

                availableUntil:
                  this.availableUntil
              }
            }
          )
        );


      // -----------------------------------------------------
      // IMPORTANT
      // -----------------------------------------------------
      //
      // The backend response has this structure:
      //
      // response
      //   └── plan
      //        ├── summary
      //        └── plan
      //             └── items[]
      //
      // -----------------------------------------------------

      console.log(
        '[MindMate Planner] Response received:',
        response
      );


      console.log(
        '[MindMate Planner] Items:',
        response?.plan?.plan?.items
      );


      // -----------------------------------------------------
      // ENTER ANGULAR ZONE
      // -----------------------------------------------------

      this.ngZone.run(() => {

        this.plan = response;

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
      //
      // Even if something unexpected happens,
      // the loading state cannot remain stuck.
      //
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
    error: unknown
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


    return (
      'Unable to generate your daily plan.'
    );

  }


  // =========================================================
  // FORCE VIEW UPDATE
  // =========================================================

  private refreshView(): void {

    this.cdr.detectChanges();

  }

}