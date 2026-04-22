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
import { MatIconButton } from '@angular/material/button';
import { MatChip } from '@angular/material/chips';
import { MatIcon } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { FilterChipComponent } from '../filter-chip/filter-chip.component';
import { FilterCondition } from '../filter.model';

@Component({
  selector: 'gd-dynamic-filter-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [FilterChipComponent, MatChip, MatIconButton, MatIcon, MatTooltipModule],
  templateUrl: './dynamic-filter-bar.component.html',
  styleUrl: './dynamic-filter-bar.component.scss',
})
export class DynamicFilterBarComponent {
  conditions = input<FilterCondition[]>([]);
  editable = input<boolean>(true);

  addRequested = output<void>();
  editRequested = output<{ index: number; condition: FilterCondition }>();
  removeRequested = output<number>();
  clearRequested = output<void>();

  protected hasConditions = computed(() => this.conditions().length > 0);
}
