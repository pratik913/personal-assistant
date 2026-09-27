import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type Theme =
  'light' |
  'dark';

export type PlanningStyle =
  'balanced' |
  'focused' |
  'flexible';

export interface UserPreferenceResponse {
  theme: Theme;
  planningStyle: PlanningStyle;
}

export interface UpdateUserPreferenceRequest {
  theme?: Theme;
  planningStyle?: PlanningStyle;
}

@Injectable({
  providedIn: 'root'
})
export class UserPreferenceService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/users/me/preferences';

  getPreferences():
    Observable<UserPreferenceResponse> {

    return this.http.get<UserPreferenceResponse>(
      this.baseUrl
    );
  }

  updatePreferences(
    request: UpdateUserPreferenceRequest
  ): Observable<UserPreferenceResponse> {

    return this.http.patch<UserPreferenceResponse>(
      this.baseUrl,
      request
    );
  }
}