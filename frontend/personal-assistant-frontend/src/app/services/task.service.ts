import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import {
  Injectable,
  inject
} from '@angular/core';

import {
  Observable
} from 'rxjs';


/* =========================================================
   TASK TYPES
   ========================================================= */

export type TaskStatus =
  | 'TODO'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

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

  goalId: string | null;
}


export interface CreateTaskRequest {

  title: string;

  description?: string;

  priority: TaskPriority;

  dueDate?: string;

  estimatedMinutes?: number;

  captureId?: string;

  goalId?: string;
}


export interface UpdateTaskRequest {

  title?: string;

  description?: string;

  status?: TaskStatus;

  priority?: TaskPriority;

  dueDate?: string;

  estimatedMinutes?: number;

  goalId?: string;
}


/* =========================================================
   TASK EXECUTION TYPES
   ========================================================= */

export type TaskExecutionStatus =
  | 'STARTED'
  | 'COMPLETED'
  | 'CANCELLED';


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


/* =========================================================
   EXECUTION AI ANALYSIS
   ========================================================= */

export type ExecutionDifficulty =
  | 'EASY'
  | 'MEDIUM'
  | 'HARD';


export type ExecutionTimeAssessment =
  | 'UNDERESTIMATED'
  | 'ACCURATE'
  | 'OVERESTIMATED';


export type ExecutionBlocker =
  | 'NONE'
  | 'KNOWLEDGE_GAP'
  | 'TECHNICAL_ISSUE'
  | 'DISTRACTION'
  | 'UNCLEAR_REQUIREMENT'
  | 'EXTERNAL_DEPENDENCY'
  | 'OTHER';


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


/* =========================================================
   DAY 17 — ADAPTIVE PLANNING
   ========================================================= */

export type PlanningConfidence =
  | 'NONE'
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH';


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


/* =========================================================
   DAY 18 — AI DAILY PLANNER
   ========================================================= */

export interface AiPlanItem {

  taskId: string;

  startAt: string;

  endAt: string;
}


export interface AiPlanData {

  planningDate: string;

  availableFrom: string;

  availableUntil: string;

  items: AiPlanItem[];
}


export interface AiPlanResponse {

  id: string;

  summary: string;

  planData: AiPlanData;

  createdAt: string;

  updatedAt?: string;
}


export interface DailyPlannerResponse {

  planningDate: string;

  availableFrom: string;

  availableUntil: string;

  plan: AiPlanResponse;
}


/* =========================================================
   TASK SERVICE
   ========================================================= */

@Injectable({
  providedIn: 'root'
})
export class TaskService {

  private readonly http =
    inject(HttpClient);


  private readonly baseUrl =
    'http://localhost:8080/api/tasks';


  private readonly dailyPlannerBaseUrl =
    'http://localhost:8080/api/daily-planner';


  /* =======================================================
     TASK CRUD
     ======================================================= */

  createTask(
    request: CreateTaskRequest
  ): Observable<TaskResponse> {

    return this.http.post<TaskResponse>(
      this.baseUrl,
      request
    );
  }


  getTasks(): Observable<TaskResponse[]> {

    return this.http.get<TaskResponse[]>(
      this.baseUrl
    );
  }


  getTask(
    taskId: string
  ): Observable<TaskResponse> {

    return this.http.get<TaskResponse>(
      `${this.baseUrl}/${taskId}`
    );
  }


  updateTask(
    taskId: string,
    request: UpdateTaskRequest
  ): Observable<TaskResponse> {

    return this.http.patch<TaskResponse>(
      `${this.baseUrl}/${taskId}`,
      request
    );
  }


  deleteTask(
    taskId: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.baseUrl}/${taskId}`
    );
  }


  removeTaskFromGoal(
    taskId: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.baseUrl}/${taskId}/goal`
    );
  }


  getTasksByCapture(
    captureId: string
  ): Observable<TaskResponse[]> {

    return this.http.get<TaskResponse[]>(
      `${this.baseUrl}/capture/${captureId}`
    );
  }


  /* =======================================================
     AI GENERATED TASKS
     ======================================================= */

  createTasksFromAiAnalysis(
    captureId: string,
    analysis: unknown
  ): Observable<TaskResponse[]> {

    return this.http.post<TaskResponse[]>(
      `${this.baseUrl}/capture/${captureId}/ai`,
      analysis
    );
  }


  /* =======================================================
     TASK EXECUTION
     ======================================================= */

  getTaskExecutions(
    taskId: string
  ): Observable<TaskExecutionResponse[]> {

    return this.http.get<TaskExecutionResponse[]>(
      `${this.baseUrl}/${taskId}/executions`
    );
  }


  startTaskExecution(
    taskId: string,
    request?: CreateTaskExecutionRequest
  ): Observable<TaskExecutionResponse> {

    return this.http.post<TaskExecutionResponse>(
      `${this.baseUrl}/${taskId}/executions`,
      request ?? {}
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


  /* =======================================================
     EXECUTION AI ANALYSIS
     ======================================================= */

  analyzeExecution(
    taskId: string,
    executionId: string
  ): Observable<ExecutionAnalysisResponse> {

    return this.http.post<ExecutionAnalysisResponse>(
      `${this.baseUrl}/${taskId}/executions/${executionId}/analysis`,
      {}
    );
  }


  getExecutionAnalysis(
    taskId: string,
    executionId: string
  ): Observable<ExecutionAnalysisResponse> {

    return this.http.get<ExecutionAnalysisResponse>(
      `${this.baseUrl}/${taskId}/executions/${executionId}/analysis`
    );
  }


  /* =======================================================
     DAY 17 — ADAPTIVE PLANNING INSIGHT
     ======================================================= */

  getPlanningInsight(
    taskId: string
  ): Observable<TaskPlanningInsightResponse> {

    return this.http.get<TaskPlanningInsightResponse>(
      `${this.baseUrl}/${taskId}/planning-insight`
    );
  }


  /* =======================================================
     DAY 18 — DAILY SMART PLANNER
     ======================================================= */

  generateDailyPlan(
    planningDate: string,
    availableFrom: string,
    availableUntil: string
  ): Observable<DailyPlannerResponse> {

    const params =
      new HttpParams()
        .set(
          'planningDate',
          planningDate
        )
        .set(
          'availableFrom',
          availableFrom
        )
        .set(
          'availableUntil',
          availableUntil
        );


    return this.http.post<DailyPlannerResponse>(
      `${this.dailyPlannerBaseUrl}/generate`,
      null,
      {
        params
      }
    );
  }

}