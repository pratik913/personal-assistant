import {
  Routes
} from '@angular/router';

import {
  AppShell
} from './shared/layout/app-shell/app-shell';

import {
  LandingComponent
} from './pages/landing/landing';

import {
  LoginComponent
} from './pages/login/login';

import {
  RegisterComponent
} from './pages/register/register';

import {
  OnboardingComponent
} from './pages/onboarding/onboarding';

import {
  Dashboard
} from './pages/dashboard/dashboard';

import {
  Capture
} from './pages/capture/capture';

import {
  Tasks
} from './pages/tasks/tasks';

import {
  TaskDetails
} from './pages/task-details/task-details';

import {
  ScheduleComponent
} from './pages/schedule/schedule';

import {
  Planner
} from './pages/planner/planner';

import {
  Goals
} from './pages/goals/goals';

import {
  SettingsComponent
} from './pages/settings/settings';

import {
  GoalDetails
} from './pages/goal-details/goal-details';

import {
  NotificationInsightsComponent
} from './pages/notification-insights/notification-insights';

import {
  authGuard
} from './core/guards/auth.guard';

export const routes: Routes = [

  {
    path: '',
    component: LandingComponent
  },

  {
    path: 'login',
    component: LoginComponent
  },

  {
    path: 'register',
    component: RegisterComponent
  },

  {
    path: 'onboarding',
    component: OnboardingComponent,
    canActivate: [authGuard]
  },

  {
    path: '',
    component: AppShell,
    canActivate: [authGuard],

    children: [

      {
        path: 'dashboard',
        component: Dashboard
      },

      {
        path: 'capture',
        component: Capture
      },

      {
        path: 'tasks',
        component: Tasks
      },

      {
        path: 'tasks/:id',
        component: TaskDetails
      },

      {
        path: 'schedule',
        component: ScheduleComponent
      },

      {
        path: 'planner',
        component: Planner
      },

      {
        path: 'goals',
        component: Goals
      },

      {
        path: 'goals/:id',
        component: GoalDetails
      },

      {
        path: 'settings',
        component: SettingsComponent
      },

      {
        path: 'notification-insights',
        component: NotificationInsightsComponent
      }
    ]
  },

  {
    path: '**',
    redirectTo: ''
  }

];