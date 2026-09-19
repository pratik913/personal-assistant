import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ThemeService } from '../../../core/services/theme/theme';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar {

  readonly themeService = inject(ThemeService);

  navigationItems = [
    {
      label: 'Dashboard',
      icon: '⌂',
      route: '/dashboard'
    },
    {
      label: 'Capture',
      icon: '✦',
      route: '/capture'
    },
    {
      label: 'Tasks',
      icon: '✓',
      route: '/tasks'
    },
    {
      label: 'Schedule',
      icon: '◷',
      route: '/schedule'
    },
    {
      label: 'Planner',
      icon: '▦',
      route: '/planner'
    },
    {
      label: 'Goals',
      icon: '◎',
      route: '/goals'
    }
  ];
}