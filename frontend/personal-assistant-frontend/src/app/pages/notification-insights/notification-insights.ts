import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  NotificationBehaviorService,
  NotificationBehaviorInsightResponse
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

  readonly insight = signal<NotificationBehaviorInsightResponse | null>(
    null
  );

  readonly loading = signal(false);

  readonly error = signal('');

  constructor() {
    console.log('DAY 24 → component created');

    this.loadInsights();
  }

  loadInsights(): void {

    console.log('DAY 24 → loadInsights()');

    this.loading.set(true);
    this.error.set('');

    this.behaviorService.getInsights().subscribe({

      next: (response) => {

        console.log(
          'DAY 24 → RESPONSE:',
          response
        );

        this.insight.set(response);

        console.log(
          'DAY 24 → SIGNAL UPDATED:',
          this.insight()
        );
      },

      error: (error) => {

        console.error(
          'DAY 24 → ERROR:',
          error
        );

        this.error.set(
          `Unable to load notification insights. HTTP status: ${
            error?.status ?? 'unknown'
          }`
        );

        this.loading.set(false);
      },

      complete: () => {

        console.log(
          'DAY 24 → REQUEST COMPLETE'
        );

        this.loading.set(false);

        console.log(
          'DAY 24 → LOADING:',
          this.loading()
        );
      }
    });
  }
}