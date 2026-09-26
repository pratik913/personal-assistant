import {
  Component,
  inject,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  NotificationBehaviorService,
  NotificationBehaviorInsightResponse,
  NotificationTimingRecommendationResponse
} from '../../services/notification-behavior.service';

@Component({
  selector: 'app-notification-insights',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-insights.html',
  styleUrl: './notification-insights.scss'
})
export class NotificationInsightsComponent {

  private readonly behaviorService = inject(
    NotificationBehaviorService
  );

  readonly insight =
    signal<NotificationBehaviorInsightResponse | null>(
      null
    );

  readonly recommendation =
    signal<NotificationTimingRecommendationResponse | null>(
      null
    );

  readonly loading =
    signal(false);

  readonly recommendationLoading =
    signal(false);

  readonly applying =
    signal(false);

  readonly error =
    signal('');

  readonly recommendationError =
    signal('');

  readonly successMessage =
    signal('');

  constructor() {

    this.loadInsights();

    this.loadRecommendation();
  }

  loadInsights(): void {

    this.loading.set(true);

    this.error.set('');

    this.behaviorService
      .getInsights()
      .subscribe({

        next: (response) => {

          console.log(
            'DAY 25 → insights response:',
            response
          );

          this.insight.set(response);
        },

        error: (error) => {

          console.error(
            'DAY 25 → insights error:',
            error
          );

          this.error.set(
            'Unable to load notification insights.'
          );
        },

        complete: () => {

          this.loading.set(false);
        }
      });
  }

  loadRecommendation(): void {

    this.recommendationLoading.set(true);

    this.recommendationError.set('');

    this.behaviorService
      .getTimingRecommendation()
      .subscribe({

        next: (response) => {

          console.log(
            'DAY 25 → recommendation response:',
            response
          );

          this.recommendation.set(response);
        },

        error: (error) => {

          console.error(
            'DAY 25 → recommendation error:',
            error
          );

          this.recommendationError.set(
            'Unable to calculate reminder recommendation.'
          );
        },

        complete: () => {

          this.recommendationLoading.set(false);
        }
      });
  }

  applyRecommendation(): void {

    const currentRecommendation =
      this.recommendation();

    if (!currentRecommendation) {
      return;
    }

    if (
      currentRecommendation.recommendedReminderMinutes ===
      currentRecommendation.currentReminderMinutes
    ) {
      return;
    }

    this.applying.set(true);

    this.successMessage.set('');

    this.behaviorService
      .updateReminderMinutes(
        currentRecommendation
          .recommendedReminderMinutes
      )
      .subscribe({

        next: () => {

          console.log(
            'DAY 25 → recommendation applied'
          );

          this.successMessage.set(
            `Future task reminders will use ${currentRecommendation.recommendedReminderMinutes} minutes.`
          );

          this.recommendation.update(
            current => current
              ? {
                  ...current,
                  currentReminderMinutes:
                    current.recommendedReminderMinutes
                }
              : current
          );
        },

        error: (error) => {

          console.error(
            'DAY 25 → apply recommendation failed:',
            error
          );

          this.recommendationError.set(
            'Unable to apply the recommendation.'
          );
        },

        complete: () => {

          this.applying.set(false);
        }
      });
  }

  refresh(): void {

    this.successMessage.set('');

    this.loadInsights();

    this.loadRecommendation();
  }
}