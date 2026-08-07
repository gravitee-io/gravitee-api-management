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

import { Component, computed, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTooltip } from '@angular/material/tooltip';

import { MatTooltipOnEllipsisDirective } from '../../directives/mat-tooltip-on-ellipsis.directive';
import { BadgeComponent } from '../badge/badge.component';
import { OverflowLabelsComponent } from '../overflow-labels/overflow-labels.component';

@Component({
  selector: 'app-api-product-card',
  standalone: true,
  imports: [BadgeComponent, MatCardModule, MatTooltip, MatTooltipOnEllipsisDirective, OverflowLabelsComponent],
  templateUrl: './api-product-card.component.html',
  styleUrl: './api-product-card.component.scss',
})
export class ApiProductCardComponent {
  readonly apiProductId = input.required<string>();
  readonly title = input.required<string>();
  readonly content = input<string>();
  readonly apiNames = input.required<string[]>();

  readonly cardSelect = output<string>();

  protected readonly openDocumentationLabel = computed(
    () => $localize`:@@apiProductCardOpenDocumentation:Open documentation for ${this.title()}:apiProductName:`,
  );
  protected readonly includedApisLabel = computed(() => {
    const apiCount = this.apiNames().length;
    return apiCount === 1
      ? $localize`:@@apiProductIncludedApiCountSingular:${apiCount}:apiCount: API INCLUDED`
      : $localize`:@@apiProductIncludedApiCountPlural:${apiCount}:apiCount: APIS INCLUDED`;
  });
}
