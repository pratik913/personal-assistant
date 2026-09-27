import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, catchError, throwError, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  token: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  timezone: string;
}

export interface RegisterResponse {
  id: string;
  name: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http = inject(HttpClient);

  private readonly authBaseUrl =
    'http://localhost:8080/api/auth';

  private readonly usersBaseUrl =
    'http://localhost:8080/api/users';

  private readonly accessTokenKey =
    'access_token';

  register(
    request: RegisterRequest
  ): Observable<RegisterResponse> {

    return this.http.post<RegisterResponse>(
      this.usersBaseUrl,
      request
    );
  }

  login(
    request: LoginRequest
  ): Observable<LoginResponse> {

    return this.http
      .post<LoginResponse>(
        `${this.authBaseUrl}/login`,
        request
      )
      .pipe(
        tap(response => {
          this.storeToken(response.token);
        }),
        catchError((error: HttpErrorResponse) => {
          return throwError(() =>
            error
          );
        })
      );
  }

  logout(): void {
    localStorage.removeItem(
      this.accessTokenKey
    );

    sessionStorage.removeItem(
      this.accessTokenKey
    );
  }

  storeToken(
    token: string,
    rememberMe = true
  ): void {

    this.logout();

    if (rememberMe) {

      localStorage.setItem(
        this.accessTokenKey,
        token
      );

      return;
    }

    sessionStorage.setItem(
      this.accessTokenKey,
      token
    );
  }

  getToken(): string | null {

    return (
      localStorage.getItem(
        this.accessTokenKey
      ) ??
      sessionStorage.getItem(
        this.accessTokenKey
      )
    );
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getErrorMessage(
    error: unknown,
    fallback = 'Something went wrong. Please try again.'
  ): string {

    if (
      error instanceof HttpErrorResponse
    ) {

      if (
        typeof error.error === 'string' &&
        error.error.trim()
      ) {
        return error.error;
      }

      if (
        error.error?.message
      ) {
        return error.error.message;
      }

      if (error.status === 400) {
        return 'Please check the information you entered.';
      }

      if (error.status === 401) {
        return 'Invalid email or password.';
      }

      if (error.status === 409) {
        return 'An account with this email already exists.';
      }

      if (error.status === 0) {
        return 'Unable to connect to MindMate backend.';
      }
    }

    return fallback;
  }
}