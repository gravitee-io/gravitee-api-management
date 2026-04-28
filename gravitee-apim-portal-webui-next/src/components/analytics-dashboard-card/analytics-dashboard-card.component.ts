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

import { DatePipe, KeyValuePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTooltip } from '@angular/material/tooltip';

import { Dashboard } from '@gravitee/gravitee-dashboard';

import { BadgeComponent } from '../badge/badge.component';

@Component({
  selector: 'app-analytics-dashboard-card',
  imports: [MatCardModule, MatTooltip, KeyValuePipe, DatePipe, BadgeComponent],
  templateUrl: './analytics-dashboard-card.component.html',
  styleUrl: './analytics-dashboard-card.component.scss',
})
export class AnalyticsDashboardCardComponent {
  readonly dashboard = input.required<Dashboard>();

  readonly cardSelect = output<string>();

  protected readonly name = computed(() => this.dashboard().name);
  protected readonly dashboardId = computed(() => this.dashboard().id);
  protected readonly labels = computed(() => this.dashboard().labels ?? {});
  protected readonly widgetCount = computed(() => this.dashboard().widgets?.length ?? 0);
  protected readonly lastModified = computed(() => this.dashboard().lastModified);
  protected readonly ariaLabel = computed(() => $localize`:@@analyticsDashboardCardAriaLabel:Open dashboard ${this.name()}:name:`);
}
