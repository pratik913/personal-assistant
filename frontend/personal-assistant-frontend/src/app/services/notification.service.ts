import { Injectable, inject } from '@angular/core';
import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

export type NotificationType =
  'TASK_STARTING';

export type NotificationStatus =
  'UNREAD' |
  'READ';


export interface NotificationResponse {

  id: string;

  taskId: string;

  taskTitle: string;

  type: NotificationType;

  status: NotificationStatus;

  title: string;

  message: string;

  scheduledAt: string;

  readAt: string | null;

  createdAt: string;

}


export interface UnreadNotificationCountResponse {

  count: number;

}


@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private readonly http =
    inject(HttpClient);


  private readonly baseUrl =
    'http://localhost:8080/api/notifications';


  getUnreadNotifications():
    Observable<NotificationResponse[]> {

    return this.http.get<
      NotificationResponse[]
    >(
      `${this.baseUrl}/unread`
    );

  }


  getUnreadCount():
    Observable<UnreadNotificationCountResponse> {

    return this.http.get<
      UnreadNotificationCountResponse
    >(
      `${this.baseUrl}/unread/count`
    );

  }


  markAsRead(
    notificationId: string
  ):
    Observable<NotificationResponse> {

    return this.http.patch<
      NotificationResponse
    >(
      `${this.baseUrl}/${notificationId}/read`,
      {}
    );

  }

}