import { Routes } from '@angular/router';

import { AppShell } from './shared/layout/app-shell/app-shell';

import { Landing } from './pages/landing/landing';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Onboarding } from './pages/onboarding/onboarding';

import { Dashboard } from './pages/dashboard/dashboard';
import { Capture } from './pages/capture/capture';
import { Tasks } from './pages/tasks/tasks';
import { TaskDetails } from './pages/task-details/task-details';
import { Schedule } from './pages/schedule/schedule';
import { Planner } from './pages/planner/planner';
import { Goals } from './pages/goals/goals';
import { Settings } from './pages/settings/settings';

export const routes: Routes = [

  {
    path: '',
    component: Landing
  },

  {
    path: 'login',
    component: Login
  },

  {
    path: 'register',
    component: Register
  },

  {
    path: 'onboarding',
    component: Onboarding
  },

  {
    path: '',
    component: AppShell,
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
        component: Schedule
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
        path: 'settings',
        component: Settings
      }
    ]
  },

  {
    path: '**',
    redirectTo: ''
  }

];