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

@Injectable({
  providedIn: 'root'
})
export class NotificationBehaviorService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/notification-insights';

  getInsights(): Observable<NotificationBehaviorInsightResponse> {
    console.log('DAY 24 → calling:', this.baseUrl);

    return this.http.get<NotificationBehaviorInsightResponse>(
      this.baseUrl
    );
  }
}