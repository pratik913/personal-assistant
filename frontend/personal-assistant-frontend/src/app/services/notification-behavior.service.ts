import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NotificationBehaviorInsightResponse {
  totalReminders: number;
  readReminders: number;
  ignoredReminders: number;
  actedOnReminders: number;
  readRate: number;
  actionRate: number;
  averageMinutesToStart: number;
  insight: string;
}

export type RecommendationConfidence =
  'NONE' |
  'LOW' |
  'MEDIUM' |
  'HIGH';

export interface NotificationTimingRecommendationResponse {
  currentReminderMinutes: number;
  recommendedReminderMinutes: number;
  executionCount: number;
  averageMinutesToStart: number;
  confidence: RecommendationConfidence;
  reason: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationBehaviorService {

  private readonly http = inject(HttpClient);

  private readonly insightsUrl =
    'http://localhost:8080/api/notification-insights';

  private readonly preferencesUrl =
    'http://localhost:8080/api/notification-preferences';

  getInsights(): Observable<NotificationBehaviorInsightResponse> {

    console.log(
      'DAY 25 → GET notification insights'
    );

    return this.http.get<NotificationBehaviorInsightResponse>(
      this.insightsUrl
    );
  }

  getTimingRecommendation():
    Observable<NotificationTimingRecommendationResponse> {

    console.log(
      'DAY 25 → GET timing recommendation'
    );

    return this.http.get<NotificationTimingRecommendationResponse>(
      `${this.insightsUrl}/recommendation`
    );
  }

  updateReminderMinutes(
    reminderMinutes: number
  ): Observable<unknown> {

    console.log(
      'DAY 25 → Updating reminder minutes:',
      reminderMinutes
    );

    return this.http.patch(
      this.preferencesUrl,
      {
        reminderMinutes
      }
    );
  }
}