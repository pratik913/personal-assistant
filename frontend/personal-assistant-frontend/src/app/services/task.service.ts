import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TaskStatus =
  | 'TODO'
  | 'IN_PROGRESS'
  | 'COMPLETED';

export type TaskPriority =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH';

export interface TaskResponse {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  estimatedMinutes: number | null;
  createdAt: string;
  updatedAt: string;
  captureId: string | null;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: TaskPriority;
  dueDate?: string | null;
  estimatedMinutes?: number | null;
  status?: TaskStatus;
}

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/tasks';

  getTasks(): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(
      this.baseUrl
    );
  }

  getTask(id: string): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(
      `${this.baseUrl}/${id}`
    );
  }

  updateTask(
    id: string,
    request: UpdateTaskRequest
  ): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(
      `${this.baseUrl}/${id}`,
      request
    );
  }

  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/${id}`
    );
  }
}