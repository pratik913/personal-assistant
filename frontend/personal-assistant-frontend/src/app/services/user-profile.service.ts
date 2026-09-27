import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserProfileResponse {
  id: string;
  name: string;
  email: string;
  phoneNumber: string | null;
  phoneNumberVerified: boolean;
  timezone: string;
  hasProfileImage: boolean;
}

export interface UpdateUserProfileRequest {
  name: string;
  email: string;
  phoneNumber: string | null;
  timezone: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserProfileService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/users/me';

  /**
   * Get the currently authenticated user's profile.
   */
  getProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(
      this.baseUrl
    );
  }

  /**
   * Update the currently authenticated user's profile.
   */
  updateProfile(
    request: UpdateUserProfileRequest
  ): Observable<UserProfileResponse> {

    return this.http.patch<UserProfileResponse>(
      this.baseUrl,
      request
    );
  }

  /**
   * Upload a new profile picture.
   *
   * The image is sent as multipart/form-data.
   */
  uploadProfileImage(
    file: File
  ): Observable<UserProfileResponse> {

    const formData = new FormData();

    formData.append(
      'file',
      file,
      file.name
    );

    return this.http.post<UserProfileResponse>(
      `${this.baseUrl}/profile-image`,
      formData
    );
  }

  /**
   * Download the profile picture from the backend.
   *
   * IMPORTANT:
   * responseType must be 'blob' because the backend
   * returns actual image bytes, not JSON.
   */
  getProfileImage(): Observable<Blob> {

    return this.http.get(
      `${this.baseUrl}/profile-image`,
      {
        responseType: 'blob'
      }
    );
  }
}