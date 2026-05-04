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

import { DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

import { Dashboard } from '@gravitee/gravitee-dashboard';

import { BadgeComponent } from '../badge/badge.component';

@Component({
  selector: 'app-analytics-dashboard-card',
  imports: [MatCardModule, MatTooltip, DatePipe, BadgeComponent, MatIconButton, MatIcon],
  templateUrl: './analytics-dashboard-card.component.html',
  styleUrl: './analytics-dashboard-card.component.scss',
})
export class AnalyticsDashboardCardComponent {
  private static readonly MAX_VISIBLE_LABELS = 2;

  readonly dashboard = input.required<Dashboard>();
  readonly isPinned = input<boolean>(false);
  readonly canPin = input<boolean>(true);
  readonly showAccent = input<boolean>(false);

  readonly cardSelect = output<string>();
  readonly pinToggle = output<string>();

  protected readonly name = computed(() => this.dashboard().name);
  protected readonly dashboardId = computed(() => this.dashboard().id);
  protected readonly widgetCount = computed(() => this.dashboard().widgets?.length ?? 0);
  protected readonly lastModified = computed(() => this.dashboard().lastModified);
  protected readonly ariaLabel = computed(() => $localize`:@@analyticsDashboardCardAriaLabel:Open dashboard ${this.name()}:name:`);
  protected readonly pinLabel = $localize`:@@analyticsDashboardPin:Pin dashboard`;
  protected readonly unpinLabel = $localize`:@@analyticsDashboardUnpin:Unpin dashboard`;

  private readonly labelEntries = computed(() => Object.entries(this.dashboard().labels ?? {}));

  protected readonly visibleLabels = computed(() =>
    this.labelEntries()
      .slice(0, AnalyticsDashboardCardComponent.MAX_VISIBLE_LABELS)
      .map(([k, v]) => `${k}:${v}`),
  );

  protected readonly hiddenLabelsCount = computed(() =>
    Math.max(0, this.labelEntries().length - AnalyticsDashboardCardComponent.MAX_VISIBLE_LABELS),
  );

  protected readonly hiddenLabelsTooltip = computed(() =>
    this.labelEntries()
      .slice(AnalyticsDashboardCardComponent.MAX_VISIBLE_LABELS)
      .map(([k, v]) => `${k}:${v}`)
      .join(', '),
  );

  onPinClick(event: Event): void {
    event.stopPropagation();
    this.pinToggle.emit(this.dashboardId());
  }
}
