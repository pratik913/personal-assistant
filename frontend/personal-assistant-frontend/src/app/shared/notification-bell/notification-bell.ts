import {
  CommonModule
} from '@angular/common';

import {
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';

import {
  Router
} from '@angular/router';

import {
  Subject,
  interval,
  startWith,
  switchMap,
  takeUntil
} from 'rxjs';

import {
  NotificationResponse,
  NotificationService
} from '../../services/notification.service';


@Component({
  selector: 'app-notification-bell',

  imports: [
    CommonModule
  ],

  templateUrl: './notification-bell.html',

  styleUrl: './notification-bell.scss'
})
export class NotificationBell
  implements OnInit, OnDestroy {


  private readonly notificationService =
    inject(NotificationService);


  private readonly router =
    inject(Router);


  private readonly destroy$ =
    new Subject<void>();


  notifications:
    NotificationResponse[] = [];


  unreadCount = 0;


  isOpen = false;


  ngOnInit(): void {

    interval(30_000)
      .pipe(

        startWith(0),

        switchMap(() =>
          this.notificationService
            .getUnreadNotifications()
        ),

        takeUntil(
          this.destroy$
        )

      )
      .subscribe({

        next: (
          notifications
        ) => {

          this.notifications =
            notifications;

          this.unreadCount =
            notifications.length;

        },

        error: (
          error
        ) => {

          console.error(
            'Unable to load notifications:',
            error
          );

        }

      });

  }


  toggle(): void {

    this.isOpen =
      !this.isOpen;

  }


  close(): void {

    this.isOpen = false;

  }


  openNotification(
    notification: NotificationResponse
  ): void {

    this.notificationService
      .markAsRead(
        notification.id
      )
      .subscribe({

        next: () => {

          this.notifications =
            this.notifications.filter(
              current =>
                current.id !==
                notification.id
            );


          this.unreadCount =
            this.notifications.length;


          this.isOpen =
            false;


          this.router.navigate([
            '/tasks',
            notification.taskId
          ]);

        },

        error: (
          error
        ) => {

          console.error(
            'Unable to mark notification as read:',
            error
          );


          this.isOpen =
            false;


          this.router.navigate([
            '/tasks',
            notification.taskId
          ]);

        }

      });

  }


  formatNotificationTime(
    value: string
  ): string {

    const date =
      new Date(value);


    if (
      Number.isNaN(
        date.getTime()
      )
    ) {

      return '';

    }


    return new Intl.DateTimeFormat(
      undefined,
      {
        hour: 'numeric',
        minute: '2-digit'
      }
    ).format(date);

  }


  ngOnDestroy(): void {

    this.destroy$.next();

    this.destroy$.complete();

  }

}