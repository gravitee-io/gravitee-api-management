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
import { NgClass } from '@angular/common';
import { Component, computed, inject, input, InputSignal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

import { CategoryCardComponent } from '../../../components/category-card/category-card.component';
import { Category } from '../../../entities/categories/categories';
import { ObservabilityBreakpointService } from '../../../services/observability-breakpoint.service';

@Component({
  selector: 'app-categories-view',
  standalone: true,
  imports: [MatCardModule, CategoryCardComponent, MatButton, RouterLink, NgClass],
  templateUrl: './categories-view.component.html',
  styleUrl: './categories-view.component.scss',
})
export class CategoriesViewComponent {
  categories: InputSignal<Category[]> = input<Category[]>([]);

  categoriesViewContainerClasses = computed(() => ({
    'categories-view__container--mobile': this.isMobile(),
  }));
  protected readonly isMobile = inject(ObservabilityBreakpointService).isMobile;
}
