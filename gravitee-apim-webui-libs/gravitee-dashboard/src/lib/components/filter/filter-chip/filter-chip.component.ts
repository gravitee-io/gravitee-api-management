/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ChangeDetectionStrategy, Component, computed, input, output, ViewEncapsulation } from '@angular/core';
import { MatChip, MatChipRemove } from '@angular/material/chips';
import { MatIcon } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { buildChipLabel, buildChipTooltip, FilterCondition } from '../filter.model';

@Component({
  selector: 'gd-filter-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [MatChip, MatChipRemove, MatIcon, MatTooltipModule],
  templateUrl: './filter-chip.component.html',
  styleUrl: './filter-chip.component.scss',
})
export class FilterChipComponent {
  filter = input.required<FilterCondition>();
  removed = output<void>();
  clicked = output<void>();

  protected label = computed(() => buildChipLabel(this.filter()));
  protected tooltip = computed(() => buildChipTooltip(this.filter()));
  protected ariaLabel = computed(() => `Edit filter: ${this.label()}. Press Delete to remove.`);
}
