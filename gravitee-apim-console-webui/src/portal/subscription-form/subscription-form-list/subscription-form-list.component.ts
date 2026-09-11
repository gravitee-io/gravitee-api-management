/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, computed, input, output, signal } from '@angular/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';

import { SubscriptionForm } from '../../../entities/management-api-v2';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

/**
 * Presentation-only catalog of the subscription forms of the environment: search, selection highlight and
 * per-row enable toggle. Every action is delegated to the parent through outputs.
 */
@Component({
  selector: 'subscription-form-list',
  imports: [MatTableModule, MatSlideToggleModule, GioTableWrapperModule],
  templateUrl: './subscription-form-list.component.html',
  styleUrl: './subscription-form-list.component.scss',
})
export class SubscriptionFormListComponent {
  readonly forms = input.required<SubscriptionForm[]>();
  readonly selectedFormId = input<string | null>(null);
  readonly canUpdate = input(false);

  readonly selectForm = output<SubscriptionForm>();
  readonly toggleEnabled = output<SubscriptionForm>();

  readonly displayedColumns = ['name', 'apis', 'enabled'];
  readonly filters = signal<GioTableWrapperFilters>({ pagination: { index: 1, size: 10 }, searchTerm: '' });

  private readonly filteredForms = computed<SubscriptionForm[]>(() => {
    const term = this.filters().searchTerm?.trim().toLowerCase() ?? '';
    const forms = this.forms();
    return term ? forms.filter(form => form.name.toLowerCase().includes(term)) : forms;
  });
  readonly total = computed(() => this.filteredForms().length);
  readonly pagedForms = computed<SubscriptionForm[]>(() => {
    const { index, size } = this.filters().pagination;
    const start = (index - 1) * size;
    return this.filteredForms().slice(start, start + size);
  });

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters.set(filters);
  }
}
