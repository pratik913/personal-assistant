import {
  Component
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  Router
} from '@angular/router';

import {
  AuthService
} from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class RegisterComponent {

  name = '';

  email = '';

  password = '';

  confirmPassword = '';

  showPassword = false;

  showConfirmPassword = false;

  acceptedTerms = false;

  isSubmitting = false;

  errorMessage = '';

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  togglePassword(): void {

    this.showPassword =
      !this.showPassword;
  }

  toggleConfirmPassword(): void {

    this.showConfirmPassword =
      !this.showConfirmPassword;
  }

  register(): void {

    this.errorMessage = '';

    const trimmedName =
      this.name.trim();

    const trimmedEmail =
      this.email.trim().toLowerCase();

    if (!trimmedName) {

      this.errorMessage =
        'Please enter your name.';

      return;
    }

    if (!trimmedEmail) {

      this.errorMessage =
        'Please enter your email.';

      return;
    }

    if (!this.isValidEmail(trimmedEmail)) {

      this.errorMessage =
        'Please enter a valid email address.';

      return;
    }

    if (this.password.length < 8) {

      this.errorMessage =
        'Password must contain at least 8 characters.';

      return;
    }

    if (
      this.password !==
      this.confirmPassword
    ) {

      this.errorMessage =
        'Passwords do not match.';

      return;
    }

    if (!this.acceptedTerms) {

      this.errorMessage =
        'Please accept the terms to continue.';

      return;
    }

    this.isSubmitting = true;

    const request = {
      name: trimmedName,
      email: trimmedEmail,
      password: this.password,
      timezone: this.getBrowserTimezone()
    };

    this.authService
      .register(request)
      .subscribe({

        next: () => {

          /*
           * Registration API creates the account
           * but intentionally does not return a JWT.
           *
           * Therefore we immediately perform login
           * so onboarding can use authenticated APIs.
           */
          this.authService
            .login({
              email: trimmedEmail,
              password: this.password
            })
            .subscribe({

              next: () => {

                this.isSubmitting = false;

                this.router.navigate([
                  '/onboarding'
                ]);
              },

              error: (error) => {

                this.isSubmitting = false;

                this.errorMessage =
                  this.authService.getErrorMessage(
                    error,
                    'Account created, but automatic login failed. Please sign in.'
                  );

                this.router.navigate([
                  '/login'
                ]);
              }
            });
        },

        error: (error) => {

          this.isSubmitting = false;

          this.errorMessage =
            this.authService.getErrorMessage(
              error,
              'Unable to create your account.'
            );
        }
      });
  }

  goToLogin(): void {

    this.router.navigate([
      '/login'
    ]);
  }

  goToLanding(): void {

    this.router.navigate([
      '/'
    ]);
  }

  private isValidEmail(
    email: string
  ): boolean {

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      .test(email);
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
}