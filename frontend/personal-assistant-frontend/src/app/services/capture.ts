import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type CaptureType = 'TEXT' | 'URL';

export interface CreateCaptureRequest {
  type: CaptureType;
  content?: string;
  sourceUrl?: string;
}

export interface CaptureResponse {
  id: string;
  type: CaptureType;
  content: string | null;
  sourceUrl: string | null;
  storageUrl: string | null;
  transcript: string | null;
  aiStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  aiError: string | null;
  aiSummary: string | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class CaptureService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl = 'http://localhost:8080/api/captures';

  createCapture(
    request: CreateCaptureRequest
  ): Observable<CaptureResponse> {

    return this.http.post<CaptureResponse>(
      this.baseUrl,
      request
    );
  }
}