import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Sidebar } from '../sidebar/sidebar';
import { NotificationBell } from '../../notification-bell/notification-bell';

@Component({
  selector: 'app-shell',

  imports: [
    RouterOutlet,
    Sidebar,
    NotificationBell
  ],

  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss'
})
export class AppShell {}