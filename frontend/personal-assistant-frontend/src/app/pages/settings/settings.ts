import {
  CommonModule
} from '@angular/common';

import {
  Component,
  OnInit,
  inject,
  signal
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  NotificationPreferenceService,
  NotificationPreferenceResponse
} from '../../services/notification-preference.service';

import {
  UserProfileService,
  UserProfileResponse
} from '../../services/user-profile.service';

import {
  UserPreferenceService,
  UserPreferenceResponse
} from '../../services/user-preference.service';


type SettingsTab =
  | 'profile'
  | 'preferences'
  | 'notifications'
  | 'privacy';


@Component({
  selector: 'app-settings',

  standalone: true,

  imports: [
    CommonModule,
    FormsModule
  ],

  templateUrl: './settings.html',

  styleUrl: './settings.scss'
})
export class SettingsComponent implements OnInit {

  /*
   * ==============================
   * SERVICES
   * ==============================
   */

  private readonly userProfileService =
    inject(UserProfileService);

  private readonly userPreferenceService =
    inject(UserPreferenceService);

  private readonly notificationPreferenceService =
    inject(NotificationPreferenceService);


  /*
   * ==============================
   * GENERAL SETTINGS STATE
   * ==============================
   */

  readonly activeTab =
    signal<SettingsTab>('profile');


  /*
   * ==============================
   * PROFILE STATE
   * ==============================
   */

  readonly loadingProfile =
    signal(false);

  readonly savingProfile =
    signal(false);

  readonly profileError =
    signal('');

  readonly profileSuccess =
    signal('');

  /*
   * IMPORTANT:
   * This property is used by settings.html
   * for displaying the uploaded profile image.
   */
  readonly profileImageUrl =
    signal<string | null>(null);


  profile = {

    name: '',

    email: '',

    phoneNumber: '',

    timezone: 'Asia/Kolkata'

  };


  /*
   * ==============================
   * USER PREFERENCES
   * ==============================
   */

  readonly loadingPreferences =
    signal(false);

  readonly savingPreferences =
    signal(false);

  readonly preferencesError =
    signal('');

  readonly preferencesSuccess =
    signal('');


  preferences: UserPreferenceResponse = {

    theme: 'light',

    planningStyle: 'balanced'

  };


  /*
   * ==============================
   * NOTIFICATION PREFERENCES
   * ==============================
   */

  readonly loadingNotifications =
    signal(false);

  readonly savingNotifications =
    signal(false);

  readonly notificationError =
    signal('');

  readonly notificationSuccess =
    signal('');


  notifications:
    NotificationPreferenceResponse = {

      taskStartNotificationsEnabled:
        true,

      reminderMinutes:
        15,

      quietHoursEnabled:
        false,

      quietHoursStart:
        null,

      quietHoursEnd:
        null
    };


  /*
   * ==============================
   * DROPDOWN OPTIONS
   * ==============================
   */

  readonly reminderOptions = [
    5,
    10,
    15,
    30,
    60,
    120
  ];


  readonly timezoneOptions = [

    'Asia/Kolkata',

    'Asia/Dubai',

    'Asia/Singapore',

    'Europe/London',

    'Europe/Paris',

    'America/New_York',

    'America/Los_Angeles'

  ];


  /*
   * ==============================
   * LIFECYCLE
   * ==============================
   */

  ngOnInit(): void {

    this.loadProfile();

    this.loadPreferences();

    this.loadNotificationPreferences();

  }


  /*
   * ==============================
   * TAB MANAGEMENT
   * ==============================
   */

  setTab(
    tab: SettingsTab
  ): void {

    this.activeTab.set(tab);

    this.profileError.set('');

    this.profileSuccess.set('');

    this.preferencesError.set('');

    this.preferencesSuccess.set('');

    this.notificationError.set('');

    this.notificationSuccess.set('');

  }


  /*
   * ==============================
   * PROFILE
   * ==============================
   */

  loadProfile(): void {

    this.loadingProfile.set(true);

    this.profileError.set('');

    this.userProfileService
      .getProfile()
      .subscribe({

        next: (
          response: UserProfileResponse
        ) => {

          this.profile = {

            name:
              response.name ?? '',

            email:
              response.email ?? '',

            phoneNumber:
              response.phoneNumber ?? '',

            timezone:
              response.timezone ??
              'Asia/Kolkata'

          };


          /*
           * Load profile image only if
           * backend says one exists.
           */

          if (
            response.hasProfileImage
          ) {

            this.loadProfileImage();

          } else {

            this.profileImageUrl.set(
              null
            );

          }


          this.loadingProfile.set(false);

        },

        error: (
          error
        ) => {

          console.error(
            'Failed to load profile:',
            error
          );

          this.profileError.set(
            error?.error?.message ??
            'Unable to load your profile.'
          );

          this.loadingProfile.set(false);

        }

      });

  }


  saveProfile(): void {

    this.profileError.set('');

    this.profileSuccess.set('');

    const name =
      this.profile.name.trim();

    const email =
      this.profile.email.trim();

    const phoneNumber =
      this.profile.phoneNumber.trim();


    if (!name) {

      this.profileError.set(
        'Name is required.'
      );

      return;

    }


    if (!email) {

      this.profileError.set(
        'Email is required.'
      );

      return;

    }


    /*
     * E.164 validation.
     *
     * Example:
     * +919876543210
     */

    if (
      phoneNumber &&
      !/^\+[1-9]\d{7,14}$/.test(
        phoneNumber
      )
    ) {

      this.profileError.set(
        'Mobile number must use international format, e.g. +919876543210.'
      );

      return;

    }


    this.savingProfile.set(true);


    this.userProfileService
      .updateProfile({

        name,

        email,

        phoneNumber:
          phoneNumber || null,

        timezone:
          this.profile.timezone

      })
      .subscribe({

        next: (
          response
        ) => {

          this.profile = {

            name:
              response.name ?? '',

            email:
              response.email ?? '',

            phoneNumber:
              response.phoneNumber ?? '',

            timezone:
              response.timezone ??
              'Asia/Kolkata'

          };


          this.profileSuccess.set(
            'Profile updated successfully.'
          );

          this.savingProfile.set(false);

        },

        error: (
          error
        ) => {

          console.error(
            'Failed to update profile:',
            error
          );

          this.profileError.set(
            error?.error?.message ??
            'Unable to update your profile.'
          );

          this.savingProfile.set(false);

        }

      });

  }


  /*
   * ==============================
   * PROFILE IMAGE
   * ==============================
   */

  onProfileImageSelected(
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    const file =
      input.files?.[0];


    if (!file) {

      return;

    }


    this.profileError.set('');

    this.profileSuccess.set('');


    /*
     * Maximum 5 MB.
     */

    if (
      file.size >
      5 * 1024 * 1024
    ) {

      this.profileError.set(
        'Profile image must be smaller than 5 MB.'
      );

      input.value = '';

      return;

    }


    /*
     * Client-side type validation.
     */

    const allowedTypes = [

      'image/jpeg',

      'image/png',

      'image/webp'

    ];


    if (
      !allowedTypes.includes(
        file.type
      )
    ) {

      this.profileError.set(
        'Only JPG, PNG and WebP images are supported.'
      );

      input.value = '';

      return;

    }


    this.userProfileService
      .uploadProfileImage(file)
      .subscribe({

        next: (
          response
        ) => {

          /*
           * Backend successfully uploaded
           * the image to S3.
           */

          if (
            response.hasProfileImage
          ) {

            this.loadProfileImage();

          }


          this.profileSuccess.set(
            'Profile picture updated successfully.'
          );

        },

        error: (
          error
        ) => {

          console.error(
            'Failed to upload profile image:',
            error
          );

          this.profileError.set(
            error?.error?.message ??
            'Unable to upload profile image.'
          );

        }

      });


    /*
     * Allow selecting the same file
     * again later.
     */

    input.value = '';

  }


  private loadProfileImage(): void {

    this.userProfileService
      .getProfileImage()
      .subscribe({

        next: (
          blob: Blob
        ) => {

          /*
           * Release previous object URL
           * to avoid memory leaks.
           */

          const previousUrl =
            this.profileImageUrl();


          if (previousUrl) {

            URL.revokeObjectURL(
              previousUrl
            );

          }


          const url =
            URL.createObjectURL(
              blob
            );


          this.profileImageUrl.set(
            url
          );

        },

        error: (
          error
        ) => {

          console.error(
            'Failed to load profile image:',
            error
          );

          this.profileImageUrl.set(
            null
          );

        }

      });

  }


  /*
   * ==============================
   * USER PREFERENCES
   * ==============================
   */

loadPreferences(): void {

  this.loadingPreferences.set(true);

  this.preferencesError.set('');

  this.userPreferenceService
    .getPreferences()
    .subscribe({

      next: (
        response: UserPreferenceResponse
      ) => {

        /*
         * Preserve the theme that is already
         * active in the application.
         *
         * This prevents Settings from changing
         * dark mode back to light mode simply
         * because the page was opened.
         */
        const currentTheme =
          document.documentElement
            .getAttribute('data-theme');

        const activeTheme:
          'light' | 'dark' =
          currentTheme === 'dark'
            ? 'dark'
            : currentTheme === 'light'
              ? 'light'
              : response.theme;


        this.preferences = {

          ...response,

          theme: activeTheme

        };


        /*
         * Only apply the backend theme when
         * the application does not already have
         * a theme selected.
         */
        if (!currentTheme) {

          this.applyTheme(
            response.theme
          );

        }


        this.loadingPreferences.set(false);

      },

      error: (
        error
      ) => {

        console.error(
          'Failed to load preferences:',
          error
        );

        this.preferencesError.set(
          error?.error?.message ??
          'Unable to load your preferences.'
        );

        this.loadingPreferences.set(false);

      }

    });

}


  savePreferences(): void {

    this.savingPreferences.set(
      true
    );

    this.preferencesError.set('');

    this.preferencesSuccess.set('');


    this.userPreferenceService
      .updatePreferences({

        theme:
          this.preferences.theme,

        planningStyle:
          this.preferences.planningStyle

      })
      .subscribe({

        next: (
          response
        ) => {

          this.preferences =
            response;


          this.applyTheme(
            response.theme
          );


          this.preferencesSuccess.set(
            'Preferences saved successfully.'
          );


          this.savingPreferences.set(
            false
          );

        },

        error: (
          error
        ) => {

          console.error(
            'Failed to save preferences:',
            error
          );

          this.preferencesError.set(
            error?.error?.message ??
            'Unable to save your preferences.'
          );


          this.savingPreferences.set(
            false
          );

        }

      });

  }


  /*
   * ==============================
   * NOTIFICATION PREFERENCES
   * ==============================
   */

  loadNotificationPreferences(): void {

    this.loadingNotifications.set(
      true
    );

    this.notificationError.set('');


    this.notificationPreferenceService
      .getPreferences()
      .subscribe({

        next: (
          response
        ) => {

          this.notifications =
            response;


          this.loadingNotifications.set(
            false
          );

        },

        error: (
          error
        ) => {

          console.error(
            'Failed to load notification preferences:',
            error
          );

          this.notificationError.set(
            error?.error?.message ??
            'Unable to load notification preferences.'
          );


          this.loadingNotifications.set(
            false
          );

        }

      });

  }


  saveNotificationPreferences(): void {

    this.savingNotifications.set(
      true
    );

    this.notificationError.set('');

    this.notificationSuccess.set('');


    /*
     * Validate quiet hours before
     * sending the request.
     */

    if (
      this.notifications
        .quietHoursEnabled
    ) {

      if (
        !this.notifications
          .quietHoursStart ||
        !this.notifications
          .quietHoursEnd
      ) {

        this.notificationError.set(
          'Please select both quiet-hours start and end time.'
        );

        this.savingNotifications.set(
          false
        );

        return;

      }


      if (
        this.notifications
          .quietHoursStart ===
        this.notifications
          .quietHoursEnd
      ) {

        this.notificationError.set(
          'Quiet-hours start and end cannot be the same.'
        );

        this.savingNotifications.set(
          false
        );

        return;

      }

    }


    this.notificationPreferenceService
      .updatePreferences({

        taskStartNotificationsEnabled:
          this.notifications
            .taskStartNotificationsEnabled,

        reminderMinutes:
          this.notifications
            .reminderMinutes,

        quietHoursEnabled:
          this.notifications
            .quietHoursEnabled,

        quietHoursStart:
          this.notifications
            .quietHoursStart ?? undefined,

        quietHoursEnd:
          this.notifications
            .quietHoursEnd ?? undefined

      })
      .subscribe({

        next: (
          response
        ) => {

          this.notifications =
            response;


          this.notificationSuccess.set(
            'Notification preferences saved successfully.'
          );


          this.savingNotifications.set(
            false
          );

        },

        error: (
          error
        ) => {

          console.error(
            'Failed to save notification preferences:',
            error
          );


          this.notificationError.set(
            error?.error?.message ??
            'Unable to save notification preferences.'
          );


          this.savingNotifications.set(
            false
          );

        }

      });

  }


  /*
   * ==============================
   * THEME
   * ==============================
   */

  private applyTheme(
    theme: 'light' | 'dark'
  ): void {

    document.documentElement
      .setAttribute(
        'data-theme',
        theme
      );

  }


  /*
   * ==============================
   * PROFILE INITIALS
   * ==============================
   */

  get initials(): string {

    const name =
      this.profile.name.trim();


    if (!name) {

      return 'P';

    }


    return name

      .split(/\s+/)

      .slice(0, 2)

      .map(
        part =>
          part
            .charAt(0)
            .toUpperCase()
      )

      .join('');

  }


  /*
   * ==============================
   * LOGOUT
   * ==============================
   */

  signOut(): void {

    /*
     * For now this redirects to login.
     *
     * Once we wire Settings to the existing
     * AuthService logout flow, this method
     * should call AuthService.logout().
     */

    window.location.href =
      '/login';

  }

}