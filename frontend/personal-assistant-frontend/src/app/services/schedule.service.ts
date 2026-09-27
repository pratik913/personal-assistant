import {
  Injectable,
  inject
} from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

export interface ScheduleEntryResponse {

  id: string;

  taskId: string;

  taskTitle: string;

  startAt: string;

  endAt: string;

  createdAt: string;

  updatedAt: string;
}

export interface CreateScheduleEntryRequest {

  taskId: string;

  startAt: string;

  endAt: string;
}

export interface UpdateScheduleEntryRequest {

  taskId: string;

  startAt: string;

  endAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ScheduleService {

  private readonly http =
    inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/schedule';

  getScheduleEntries():
    Observable<ScheduleEntryResponse[]> {

    return this.http.get<
      ScheduleEntryResponse[]
    >(this.baseUrl);
  }

  getScheduleEntry(
    id: string
  ): Observable<ScheduleEntryResponse> {

    return this.http.get<
      ScheduleEntryResponse
    >(`${this.baseUrl}/${id}`);
  }

  createScheduleEntry(
    request: CreateScheduleEntryRequest
  ): Observable<ScheduleEntryResponse> {

    return this.http.post<
      ScheduleEntryResponse
    >(
      this.baseUrl,
      request
    );
  }

  updateScheduleEntry(
    id: string,
    request: UpdateScheduleEntryRequest
  ): Observable<ScheduleEntryResponse> {

    return this.http.patch<
      ScheduleEntryResponse
    >(
      `${this.baseUrl}/${id}`,
      request
    );
  }

  deleteScheduleEntry(
    id: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.baseUrl}/${id}`
    );
  }
}