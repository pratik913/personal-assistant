import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TaskStatus =
  'TODO' |
  'IN_PROGRESS' |
  'COMPLETED';

export type TaskPriority =
  'LOW' |
  'MEDIUM' |
  'HIGH';

export type TaskExecutionStatus =
  'STARTED' |
  'COMPLETED' |
  'CANCELLED';

export type ExecutionDifficulty =
  'EASY' |
  'MEDIUM' |
  'HARD';

export type ExecutionTimeAssessment =
  'UNDERESTIMATED' |
  'ACCURATE' |
  'OVERESTIMATED';

export type ExecutionBlocker =
  'NONE' |
  'KNOWLEDGE_GAP' |
  'TECHNICAL_ISSUE' |
  'DISTRACTION' |
  'UNCLEAR_REQUIREMENT' |
  'EXTERNAL_DEPENDENCY' |
  'OTHER';

export type PlanningConfidence =
  'NONE' |
  'LOW' |
  'MEDIUM' |
  'HIGH';

export interface TaskPlanningInsightResponse {
  taskId: string;
  estimatedMinutes: number | null;
  averageActualMinutes: number | null;
  executionCount: number;
  planningExecutionCount: number;
  excludedExecutionCount: number;
  recommendedMinutes: number | null;
  confidence: PlanningConfidence;
}

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
  goalId: string | null;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueDate?: string;
  estimatedMinutes?: number;
  captureId?: string;
  goalId?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: TaskPriority;
  dueDate?: string | null;
  estimatedMinutes?: number | null;
  status?: TaskStatus;
  goalId?: string;
}

export interface CreateTaskExecutionRequest {
  feedback?: string;
}

export interface UpdateTaskExecutionRequest {
  status?: TaskExecutionStatus;
  feedback?: string;
}

export interface TaskExecutionResponse {
  id: string;
  taskId: string;
  startedAt: string;
  endedAt: string | null;
  status: TaskExecutionStatus;
  feedback: string | null;
  createdAt: string;
}

export interface ExecutionAnalysisResponse {
  id: string;
  executionId: string;
  difficulty: ExecutionDifficulty;
  timeAssessment: ExecutionTimeAssessment;
  blocker: ExecutionBlocker;
  insight: string | null;
  suggestion: string | null;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class TaskService {

  private readonly http =
    inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/tasks';

  /* =======================================================
     TASK APIs
     ======================================================= */

  getTasks(): Observable<TaskResponse[]> {

    return this.http.get<TaskResponse[]>(
      this.baseUrl
    );
  }

  getTask(
    id: string
  ): Observable<TaskResponse> {

    return this.http.get<TaskResponse>(
      `${this.baseUrl}/${id}`
    );
  }

  createTask(
    request: CreateTaskRequest
  ): Observable<TaskResponse> {

    return this.http.post<TaskResponse>(
      this.baseUrl,
      request
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

  deleteTask(
    id: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.baseUrl}/${id}`
    );
  }

  removeTaskFromGoal(
    taskId: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.baseUrl}/${taskId}/goal`
    );
  }

  /* =======================================================
     TASK EXECUTION APIs
     ======================================================= */

  startTaskExecution(
    taskId: string,
    request: CreateTaskExecutionRequest = {}
  ): Observable<TaskExecutionResponse> {

    return this.http.post<TaskExecutionResponse>(
      `${this.baseUrl}/${taskId}/executions`,
      request
    );
  }

  updateTaskExecution(
    taskId: string,
    executionId: string,
    request: UpdateTaskExecutionRequest
  ): Observable<TaskExecutionResponse> {

    return this.http.patch<TaskExecutionResponse>(
      `${this.baseUrl}/${taskId}/executions/${executionId}`,
      request
    );
  }

  getTaskExecutions(
    taskId: string
  ): Observable<TaskExecutionResponse[]> {

    return this.http.get<TaskExecutionResponse[]>(
      `${this.baseUrl}/${taskId}/executions`
    );
  }

  /* =======================================================
     EXECUTION ANALYSIS APIs
     ======================================================= */

  getExecutionAnalysis(
    taskId: string,
    executionId: string
  ): Observable<ExecutionAnalysisResponse> {

    return this.http.get<ExecutionAnalysisResponse>(
      `${this.baseUrl}/${taskId}/executions/${executionId}/analysis`
    );
  }

  analyzeExecution(
    taskId: string,
    executionId: string
  ): Observable<ExecutionAnalysisResponse> {

    return this.http.post<ExecutionAnalysisResponse>(
      `${this.baseUrl}/${taskId}/executions/${executionId}/analysis`,
      {}
    );
  }

  /* =======================================================
     PLANNING INTELLIGENCE APIs
     ======================================================= */

  getPlanningInsight(
    taskId: string
  ): Observable<TaskPlanningInsightResponse> {

    return this.http.get<TaskPlanningInsightResponse>(
      `${this.baseUrl}/${taskId}/planning-insight`
    );
  }
}