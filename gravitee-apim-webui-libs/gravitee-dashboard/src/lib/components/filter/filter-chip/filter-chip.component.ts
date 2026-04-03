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

import { buildChipLabelParts, buildChipTooltip, ChipLabelParts, FilterCondition } from '../filter.model';

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
  /**
   * When `false` the chip is disabled: no ripple, no pointer cursor, the remove
   * icon is hidden and clicks are ignored. Use this for filters that are active
   * but cannot be modified by the current user (e.g. insufficient permissions).
   */
  editable = input<boolean>(true);
  removed = output<void>();
  clicked = output<void>();

  protected labelParts = computed<ChipLabelParts>(() => buildChipLabelParts(this.filter()));
  protected tooltip = computed(() => buildChipTooltip(this.filter()));
  protected ariaLabel = computed(() => {
    const expr = buildChipTooltip(this.filter());
    return this.editable() ? `Edit filter: ${expr}. Press Delete to remove.` : `Active filter: ${expr}`;
  });

  protected onChipClick(): void {
    if (this.editable()) {
      this.clicked.emit();
    }
  }

  // Prevents the chip from gaining focus when the remove icon is pressed.
  // stopPropagation blocks the chip ripple; preventDefault blocks the browser's
  // default focus transfer on mousedown. The click event still fires so
  // matChipRemove can process the removal.
  protected onRemovePointerDown(event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
  }
}
