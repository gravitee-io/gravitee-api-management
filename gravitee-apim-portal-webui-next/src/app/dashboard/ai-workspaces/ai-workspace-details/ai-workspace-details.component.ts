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
import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { ActivatedRoute } from '@angular/router';
import { map, of } from 'rxjs';

import { LoaderComponent } from '../../../../components/loader/loader.component';
import { AiWorkspaceBudget, AiWorkspaceService } from '../../../../services/ai-workspace.service';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';

@Component({
  selector: 'app-ai-workspace-details',
  imports: [DatePipe, MatButton, LoaderComponent],
  templateUrl: './ai-workspace-details.component.html',
  styleUrl: './ai-workspace-details.component.scss',
})
export default class AiWorkspaceDetailsComponent {
  private readonly service = inject(AiWorkspaceService);
  private readonly route = inject(ActivatedRoute);
  private readonly breadcrumbs = inject(BreadcrumbService);
  readonly copied = signal(false);
  readonly modelsPage = signal(1);
  readonly modelsPageSize = 10;

  private readonly aiWorkspaceId = toSignal(this.route.paramMap.pipe(map(params => params.get('aiWorkspaceId') ?? '')), {
    initialValue: '',
  });

  readonly details = rxResource({
    params: () => this.aiWorkspaceId() || null,
    stream: ({ params }) => (params ? this.service.get(params) : of(undefined)),
  });

  readonly consumption = rxResource({
    params: () => this.aiWorkspaceId() || null,
    stream: ({ params }) => (params ? this.service.consumption(params) : of(undefined)),
  });

  readonly models = computed(() => this.details.value()?.models ?? []);
  readonly visibleModels = computed(() => {
    const start = (this.modelsPage() - 1) * this.modelsPageSize;
    return this.models().slice(start, start + this.modelsPageSize);
  });
  readonly modelsHavePrevious = computed(() => this.modelsPage() > 1);
  readonly modelsHaveNext = computed(() => this.modelsPage() * this.modelsPageSize < this.models().length);

  constructor() {
    effect(onCleanup => {
      const name = this.details.value()?.name;
      this.breadcrumbs.set([
        {
          id: 'ai-workspaces',
          label: $localize`:@@aiWorkspaceMenu:My Workspace`,
          url: '/dashboard/ai-workspaces',
        },
        { id: 'ai-workspace', label: name || this.aiWorkspaceId() || $localize`:@@aiWorkspaceMenu:My Workspace` },
      ]);
      onCleanup(() => this.breadcrumbs.clear());
    });
  }

  budgetLabel(budget?: AiWorkspaceBudget): string {
    if (budget?.amount == null) {
      return $localize`:@@aiWorkspaceBudgetUnavailable:Budget unavailable`;
    }
    return budget.period ? `${budget.amount} / ${budget.period}` : `${budget.amount}`;
  }

  price(value?: number): string {
    return value == null ? '—' : `${value}`;
  }

  async copyEndpoint(): Promise<void> {
    const endpoint = this.details.value()?.endpointUrl;
    if (!endpoint) {
      return;
    }
    await navigator.clipboard.writeText(endpoint);
    this.copied.set(true);
  }
}
