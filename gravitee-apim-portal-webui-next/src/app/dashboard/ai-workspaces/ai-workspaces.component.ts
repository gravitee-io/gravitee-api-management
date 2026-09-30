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
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { AiWorkspaceBudget, AiWorkspaceService } from '../../../services/ai-workspace.service';

@Component({
  selector: 'app-ai-workspaces',
  imports: [ReactiveFormsModule, RouterLink, MatCard, MatCardContent, MatFormField, MatLabel, MatInput, MatButton, LoaderComponent],
  templateUrl: './ai-workspaces.component.html',
  styleUrl: './ai-workspaces.component.scss',
})
export default class AiWorkspacesComponent {
  private readonly service = inject(AiWorkspaceService);
  private trackedName = '';

  readonly pageSize = signal(10);
  readonly page = signal(1);
  readonly name = new FormControl('', { nonNullable: true });

  private readonly debouncedName = toSignal(
    this.name.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), startWith(this.name.value)),
    { initialValue: this.name.value },
  );

  private readonly list = rxResource({
    params: () => ({ name: this.debouncedName(), page: this.page(), size: this.pageSize() }),
    stream: ({ params }) => this.service.list(params.page, params.size, params.name),
  });

  readonly workspaces = computed(() => this.list.value()?.data ?? []);
  readonly currentPage = computed(() => this.list.value()?.page ?? this.page());
  readonly total = computed(() => this.list.value()?.total ?? 0);
  readonly isLoading = computed(() => this.list.isLoading());
  readonly hasError = computed(() => this.list.error() != null);
  readonly hasPrevious = computed(() => this.currentPage() > 1);
  readonly hasNext = computed(() => this.currentPage() * this.pageSize() < this.total());

  constructor() {
    effect(() => {
      const name = this.debouncedName();
      untracked(() => {
        if (this.trackedName === name) {
          return;
        }
        this.trackedName = name;
        if (this.page() !== 1) {
          this.page.set(1);
        }
      });
    });
  }

  setPageSize(size: number): void {
    this.pageSize.set(size);
    this.page.set(1);
  }

  onPageSizeChange(event: Event): void {
    const selected = event.target;
    if (!(selected instanceof HTMLSelectElement)) {
      return;
    }
    this.setPageSize(Number(selected.value));
  }

  previous(): void {
    this.page.set(Math.max(1, this.page() - 1));
  }

  next(): void {
    this.page.set(this.page() + 1);
  }

  budgetLabel(budget?: AiWorkspaceBudget): string {
    if (budget?.amount == null) {
      return $localize`:@@aiWorkspaceBudgetUnavailable:Budget unavailable`;
    }
    return budget.period ? `${budget.amount} / ${budget.period}` : `${budget.amount}`;
  }
}
