import {
  Component,
  DestroyRef,
  afterNextRender,
  inject,
  signal
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  Router
} from '@angular/router';

import {
  interval,
  startWith,
  switchMap
} from 'rxjs';

import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  NotificationResponse,
  NotificationService
} from '../../services/notification.service';


@Component({
  selector: 'app-notification-bell',

  standalone: true,

  imports: [
    CommonModule
  ],

  templateUrl: './notification-bell.html',

  styleUrl: './notification-bell.scss'
})
export class NotificationBell {

  private readonly notificationService =
    inject(NotificationService);

  private readonly router =
    inject(Router);

  private readonly destroyRef =
    inject(DestroyRef);


  /*
   * =====================================================
   * STATE
   * =====================================================
   */

  readonly notifications =
    signal<NotificationResponse[]>([]);

  readonly unreadCount =
    signal(0);

  readonly isOpen =
    signal(false);


  /*
   * =====================================================
   * INITIALIZATION
   * =====================================================
   */

  constructor() {

    /*
     * Start notification polling after the
     * initial Angular render.
     *
     * This prevents NG0100 caused by the
     * notification state changing during
     * the first change-detection cycle.
     */
    afterNextRender(() => {

      this.startNotificationPolling();

    });

  }


  /*
   * =====================================================
   * NOTIFICATION POLLING
   * =====================================================
   */

  private startNotificationPolling(): void {

    interval(30_000)
      .pipe(

        /*
         * Load immediately once,
         * then every 30 seconds.
         */
        startWith(0),

        switchMap(() =>
          this.notificationService
            .getUnreadNotifications()
        ),

        /*
         * Automatically unsubscribe when
         * NotificationBell is destroyed.
         */
        takeUntilDestroyed(
          this.destroyRef
        )

      )
      .subscribe({

        next: (
          notifications
        ) => {

          this.notifications.set(
            notifications
          );

          this.unreadCount.set(
            notifications.length
          );

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


  /*
   * =====================================================
   * DROPDOWN
   * =====================================================
   */

  toggle(): void {

    this.isOpen.update(
      isOpen => !isOpen
    );

  }


  close(): void {

    this.isOpen.set(false);

  }


  /*
   * =====================================================
   * OPEN NOTIFICATION
   * =====================================================
   */

  openNotification(
    notification: NotificationResponse
  ): void {

    this.notificationService
      .markAsRead(
        notification.id
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: () => {

          this.notifications.update(
            notifications =>
              notifications.filter(
                current =>
                  current.id !==
                  notification.id
              )
          );

          this.unreadCount.update(
            count =>
              Math.max(
                0,
                count - 1
              )
          );

          this.isOpen.set(false);

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

          /*
           * Preserve your existing behavior:
           * navigate even if marking as read fails.
           */
          this.isOpen.set(false);

          this.router.navigate([
            '/tasks',
            notification.taskId
          ]);

        }

      });

  }


  /*
   * =====================================================
   * TIME FORMATTER
   * =====================================================
   */

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

}