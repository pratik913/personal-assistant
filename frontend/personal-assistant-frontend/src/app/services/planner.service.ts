import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateAiPlanRequest {
  planningDate: string;
  availableFrom: string;
  availableUntil: string;
}

export interface AiPlanItem {
  taskId: string;
  taskTitle: string;
  startAt: string;
  endAt: string;
  reason: string;
}

export interface AiPlanData {
  planningDate: string;
  items: AiPlanItem[];
}

export interface AiPlanResponse {
  id: string;
  summary: string;
  plan: AiPlanData;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlannerService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/planner';

  createPlan(
    request: CreateAiPlanRequest
  ): Observable<AiPlanResponse> {

    return this.http.post<AiPlanResponse>(
      this.baseUrl,
      request
    );
  }
}