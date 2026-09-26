import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NotificationPreferenceResponse {
  taskStartNotificationsEnabled: boolean;
  reminderMinutes: number;
  quietHoursEnabled: boolean;
  quietHoursStart: string | null;
  quietHoursEnd: string | null;
}

export interface UpdateNotificationPreferenceRequest {
  taskStartNotificationsEnabled?: boolean;
  reminderMinutes?: number;
  quietHoursEnabled?: boolean;
  quietHoursStart?: string;
  quietHoursEnd?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationPreferenceService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/notification-preferences';

  getPreferences(): Observable<NotificationPreferenceResponse> {

    return this.http.get<NotificationPreferenceResponse>(
      this.baseUrl
    );
  }

  updatePreferences(
    request: UpdateNotificationPreferenceRequest
  ): Observable<NotificationPreferenceResponse> {

    return this.http.patch<NotificationPreferenceResponse>(
      this.baseUrl,
      request
    );
  }
}