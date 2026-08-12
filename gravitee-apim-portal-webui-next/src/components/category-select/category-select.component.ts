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
import { Component, computed, input, model } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';

import { PortalCategory } from '../../entities/categories/portal-category';

const ALL_LABEL = $localize`:@@catalogCategorySelectAllValue:All`;

@Component({
  selector: 'app-category-select',
  standalone: true,
  imports: [MatMenuModule],
  templateUrl: './category-select.component.html',
  styleUrl: './category-select.component.scss',
})
export class CategorySelectComponent {
  readonly categories = input.required<PortalCategory[]>();
  readonly value = model<string | null>(null);

  protected readonly selectedLabel = computed(() => this.categories().find(category => category.id === this.value())?.title ?? ALL_LABEL);

  select(categoryId: string | null): void {
    this.value.set(categoryId);
  }
}
