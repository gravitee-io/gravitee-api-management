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
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatOption, MatSelect, MatSelectChange } from '@angular/material/select';

import { FilterDefinition } from '../../filter.model';

@Component({
  selector: 'gd-enum-value-input',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './enum-value-input.component.scss',
  imports: [MatFormField, MatLabel, MatSelect, MatOption],
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="gd-value-input">
      <mat-label>Filter value</mat-label>
      <mat-select
        panelClass="gd-value-input-select-panel"
        [multiple]="isMultiSelect()"
        [value]="selectValue()"
        (selectionChange)="onSelectionChange($event)"
      >
        @for (opt of definition().values ?? []; track opt) {
          <mat-option [value]="opt">{{ opt }}</mat-option>
        }
      </mat-select>
    </mat-form-field>
  `,
})
export class EnumValueInputComponent {
  definition = input.required<FilterDefinition>();
  /** Current operator token (e.g. IN / EQ) from the parent dialog. */
  selectedOperator = input.required<string>();
  selectedValues = input<string[]>([]);
  valuesChange = output<string[]>();

  protected readonly isMultiSelect = computed(() => {
    const op = this.selectedOperator();
    return op === 'IN' || op === 'NOT_IN';
  });

  /** Value binding for `mat-select`: array when multi, first value when single. */
  protected selectValue(): string | string[] | null {
    const vals = this.selectedValues();
    if (this.isMultiSelect()) {
      return [...vals];
    }
    return vals.length > 0 ? vals[0] : null;
  }

  protected onSelectionChange(event: MatSelectChange): void {
    if (this.isMultiSelect()) {
      const next = (event.value as string[] | undefined) ?? [];
      this.valuesChange.emit([...next]);
    } else {
      const v = event.value as string | null | undefined;
      this.valuesChange.emit(v != null && v !== '' ? [v] : []);
    }
  }
}
