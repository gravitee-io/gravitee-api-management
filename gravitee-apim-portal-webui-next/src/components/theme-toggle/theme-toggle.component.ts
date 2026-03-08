/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, computed, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';

import { ThemeMode, ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.component.html',
  styleUrl: './theme-toggle.component.scss',
  imports: [MatButton, MatIcon, MatMenuTrigger, MatMenu, MatMenuItem],
})
export class ThemeToggleComponent {
  protected readonly themeService = inject(ThemeService);
  protected readonly modes: { value: ThemeMode; label: string; icon: string }[] = [
    { value: 'light', label: $localize`:@@themeToggleLight:Light`, icon: 'light_mode' },
    { value: 'dark', label: $localize`:@@themeToggleDark:Dark`, icon: 'dark_mode' },
    { value: 'system', label: $localize`:@@themeToggleSystem:System`, icon: 'contrast' },
  ];
  protected readonly currentIcon = computed(() => {
    const mode = this.themeService.themeMode();
    return this.modes.find(m => m.value === mode)?.icon ?? 'brightness_auto';
  });
}
