import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {

  private readonly storageKey = 'mindmate-theme';

  readonly theme = signal<Theme>(this.getInitialTheme());

  constructor() {
    this.applyTheme(this.theme());
  }

  toggle(): void {
    const nextTheme: Theme =
      this.theme() === 'light' ? 'dark' : 'light';

    this.setTheme(nextTheme);
  }

  setTheme(theme: Theme): void {
    this.theme.set(theme);
    localStorage.setItem(this.storageKey, theme);
    this.applyTheme(theme);
  }

  private applyTheme(theme: Theme): void {
    document.documentElement.setAttribute('data-theme', theme);
  }

  private getInitialTheme(): Theme {
    const savedTheme = localStorage.getItem(this.storageKey);

    if (savedTheme === 'light' || savedTheme === 'dark') {
      return savedTheme;
    }

    return window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  }
}