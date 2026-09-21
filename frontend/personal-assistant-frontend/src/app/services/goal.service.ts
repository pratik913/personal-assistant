import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  TaskResponse
} from './task.service';

export interface Goal {
  id: string;
  title: string;
  description: string | null;
  targetDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGoalRequest {
  title: string;
  description?: string;
  targetDate?: string;
}

export interface UpdateGoalRequest {
  title?: string;
  description?: string;
  targetDate?: string;
}

export interface GoalProgress {
  totalTasks: number;
  completedTasks: number;
  remainingTasks: number;
  progressPercentage: number;
}

@Injectable({
  providedIn: 'root'
})
export class GoalService {

  private readonly baseUrl =
    'http://localhost:8080/api/goals';

  constructor(
    private http: HttpClient
  ) {}

  getGoals(): Observable<Goal[]> {

    return this.http.get<Goal[]>(
      this.baseUrl
    );
  }

  getGoal(
    goalId: string
  ): Observable<Goal> {

    return this.http.get<Goal>(
      `${this.baseUrl}/${goalId}`
    );
  }

  createGoal(
    request: CreateGoalRequest
  ): Observable<Goal> {

    return this.http.post<Goal>(
      this.baseUrl,
      request
    );
  }

  updateGoal(
    goalId: string,
    request: UpdateGoalRequest
  ): Observable<Goal> {

    return this.http.patch<Goal>(
      `${this.baseUrl}/${goalId}`,
      request
    );
  }

  deleteGoal(
    goalId: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.baseUrl}/${goalId}`
    );
  }

  getGoalTasks(
    goalId: string
  ): Observable<TaskResponse[]> {

    return this.http.get<TaskResponse[]>(
      `${this.baseUrl}/${goalId}/tasks`
    );
  }

  getGoalProgress(
    goalId: string
  ): Observable<GoalProgress> {

    return this.http.get<GoalProgress>(
      `${this.baseUrl}/${goalId}/progress`
    );
  }
}