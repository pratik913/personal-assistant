import {
  Component,
  OnInit
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  Router
} from '@angular/router';

import {
  UserProfileService
} from '../../services/user-profile.service';

interface Interest {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './onboarding.html',
  styleUrl: './onboarding.scss'
})
export class OnboardingComponent
  implements OnInit {

  currentStep = 1;

  selectedInterests: string[] = [];

  primaryGoal = '';

  isLoading = true;

  isSaving = false;

  errorMessage = '';

  interests: Interest[] = [
    {
      label: 'Work',
      icon: '◈'
    },
    {
      label: 'Study',
      icon: '◎'
    },
    {
      label: 'Health',
      icon: '◇'
    },
    {
      label: 'Finance',
      icon: '◌'
    },
    {
      label: 'Personal Growth',
      icon: '✦'
    },
    {
      label: 'Hobbies',
      icon: '○'
    },
    {
      label: 'Family',
      icon: '♡'
    },
    {
      label: 'Other',
      icon: '＋'
    }
  ];

  goals = [
    'Become more productive',
    'Build better habits',
    'Learn new skills',
    'Prepare for career growth',
    'Manage my personal life',
    'Achieve a specific goal'
  ];

  constructor(
    private readonly router: Router,
    private readonly userProfileService:
      UserProfileService
  ) {}

  ngOnInit(): void {

    this.loadExistingOnboardingData();
  }

  toggleInterest(
    label: string
  ): void {

    if (
      this.selectedInterests
        .includes(label)
    ) {

      this.selectedInterests =
        this.selectedInterests
          .filter(
            item => item !== label
          );

      return;
    }

    this.selectedInterests = [
      ...this.selectedInterests,
      label
    ];
  }

  isSelected(
    label: string
  ): boolean {

    return this.selectedInterests
      .includes(label);
  }

  next(): void {

    this.errorMessage = '';

    if (
      this.currentStep === 1
    ) {

      if (
        this.selectedInterests.length === 0
      ) {

        this.errorMessage =
          'Please select at least one interest.';

        return;
      }

      this.currentStep = 2;

      return;
    }

    this.completeOnboarding();
  }

  back(): void {

    this.errorMessage = '';

    if (
      this.currentStep > 1
    ) {

      this.currentStep--;
    }
  }

  skip(): void {

    this.completeOnboarding();
  }

  completeOnboarding(): void {

    if (this.isSaving) {
      return;
    }

    this.errorMessage = '';

    /*
     * The current backend User model requires timezone.
     * We update the existing authenticated profile rather
     * than inventing a new onboarding endpoint.
     */
    this.isSaving = true;

    this.userProfileService
      .getProfile()
      .subscribe({

        next: profile => {

          this.userProfileService
            .updateProfile({
              name: profile.name,
              email: profile.email,
              phoneNumber: profile.phoneNumber,
              timezone: this.getBrowserTimezone()
            })
            .subscribe({

              next: () => {

                this.persistLocalOnboardingData();

                this.isSaving = false;

                this.router.navigate([
                  '/dashboard'
                ]);
              },

              error: error => {

                this.isSaving = false;

                this.errorMessage =
                  this.getErrorMessage(
                    error,
                    'Unable to save your onboarding settings.'
                  );
              }
            });
        },

        error: error => {

          this.isSaving = false;

          this.errorMessage =
            this.getErrorMessage(
              error,
              'Unable to load your profile.'
            );
        }
      });
  }

  private loadExistingOnboardingData(): void {

    try {

      const storedInterests =
        localStorage.getItem(
          'mindmate_onboarding_interests'
        );

      const storedGoal =
        localStorage.getItem(
          'mindmate_primary_goal'
        );

      if (storedInterests) {

        const parsed =
          JSON.parse(storedInterests);

        if (Array.isArray(parsed)) {

          this.selectedInterests =
            parsed;
        }
      }

      if (storedGoal) {

        this.primaryGoal =
          storedGoal;
      }

    } catch {

      /*
       * Corrupt local onboarding data should not
       * block the actual onboarding flow.
       */
    }

    this.isLoading = false;
  }

  private persistLocalOnboardingData(): void {

    localStorage.setItem(
      'mindmate_onboarding_completed',
      'true'
    );

    localStorage.setItem(
      'mindmate_onboarding_interests',
      JSON.stringify(
        this.selectedInterests
      )
    );

    localStorage.setItem(
      'mindmate_primary_goal',
      this.primaryGoal
    );
  }

  private getBrowserTimezone(): string {

    try {

      return Intl.DateTimeFormat()
        .resolvedOptions()
        .timeZone || 'UTC';

    } catch {

      return 'UTC';
    }
  }

  private getErrorMessage(
    error: any,
    fallback: string
  ): string {

    if (
      error?.error?.message
    ) {

      return error.error.message;
    }

    if (
      typeof error?.error === 'string' &&
      error.error.trim()
    ) {

      return error.error;
    }

    if (error?.status === 401) {

      return 'Your session has expired. Please sign in again.';
    }

    if (error?.status === 0) {

      return 'Unable to connect to MindMate backend.';
    }

    return fallback;
  }
}