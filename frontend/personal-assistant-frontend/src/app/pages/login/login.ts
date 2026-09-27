import {
  Component,
  inject
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

  imports: [
    FormsModule
  ],

  templateUrl: './login.html',

  styleUrl: './login.scss'
})
export class LoginComponent {

  private readonly router =
    inject(Router);

  private readonly authService =
    inject(AuthService);

  email = '';

  password = '';

  showPassword =
    false;

  rememberMe =
    true;

  isSubmitting =
    false;

  errorMessage =
    '';

  togglePassword(): void {

    this.showPassword =
      !this.showPassword;
  }

  login(): void {

    this.errorMessage =
      '';

    const email =
      this.email.trim();

    if (!email) {

      this.errorMessage =
        'Please enter your email.';

      return;
    }

    if (!this.password) {

      this.errorMessage =
        'Please enter your password.';

      return;
    }

    this.isSubmitting =
      true;

    this.authService
      .login({
        email,
        password:
          this.password
      })
      .subscribe({

        next: (
          response
        ) => {

          console.log(
            'Login successful'
          );

          /*
           * AuthService has already stored
           * the JWT in localStorage.
           *
           * Now we can safely enter the
           * authenticated application.
           */

          this.isSubmitting =
            false;

          this.router.navigate([
            '/dashboard'
          ]);
        },

        error: (
          error
        ) => {

          console.error(
            'LOGIN ERROR:',
            error
          );

          this.isSubmitting =
            false;

          if (
            error.status === 401
          ) {

            this.errorMessage =
              'Invalid email or password.';

            return;
          }

          if (
            error.status === 400
          ) {

            this.errorMessage =
              error.error?.message
              ??
              'Please check your login details.';

            return;
          }

          this.errorMessage =
            'Unable to log in right now. Please try again.';
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
}