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
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class LoginComponent {

  email = '';

  password = '';

  showPassword = false;

  rememberMe = true;

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

  login(): void {

    this.errorMessage = '';

    const trimmedEmail =
      this.email.trim().toLowerCase();

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

    if (!this.password) {

      this.errorMessage =
        'Please enter your password.';

      return;
    }

    this.isSubmitting = true;

    this.authService
      .login({
        email: trimmedEmail,
        password: this.password
      })
      .subscribe({

        next: response => {

          /*
           * AuthService stores the token by default.
           *
           * Respect the Remember Me choice by moving
           * the token to sessionStorage when requested.
           */
          this.authService.storeToken(
            response.token,
            this.rememberMe
          );

          this.isSubmitting = false;

          this.router.navigate([
            '/dashboard'
          ]);
        },

        error: error => {

          this.isSubmitting = false;

          this.errorMessage =
            this.authService.getErrorMessage(
              error,
              'Unable to sign in. Please try again.'
            );
        }
      });
  }

  goToRegister(): void {

    this.router.navigate([
      '/register'
    ]);
  }

  goToLanding(): void {

    this.router.navigate([
      '/'
    ]);
  }

  forgotPassword(): void {

    this.errorMessage =
      'Password recovery will be added in a later authentication milestone.';
  }

  private isValidEmail(
    email: string
  ): boolean {

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      .test(email);
  }
}