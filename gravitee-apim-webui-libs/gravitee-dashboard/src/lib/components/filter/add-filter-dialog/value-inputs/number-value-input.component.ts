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
import { ChangeDetectionStrategy, Component, computed, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

import { FilterDefinition } from '../../filter.model';

@Component({
  selector: 'gd-number-value-input',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatFormField, MatLabel, MatHint, MatInput],
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="gd-value-input">
      <mat-label>Filter value</mat-label>
      <input
        matInput
        type="number"
        placeholder="Enter a number"
        [min]="rangeMin() ?? null"
        [max]="rangeMax() ?? null"
        [ngModel]="numberValue()"
        (ngModelChange)="onInput($event)"
      />
      @if (hasRange()) {
        <mat-hint>{{ rangeMin() }} – {{ rangeMax() }}</mat-hint>
      }
    </mat-form-field>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .gd-value-input {
        width: 100%;
      }
    `,
  ],
})
export class NumberValueInputComponent implements OnInit {
  definition = input.required<FilterDefinition>();
  selectedValues = input<string[]>([]);
  valuesChange = output<string[]>();

  protected hasRange = computed(() => this.definition().range != null);
  protected rangeMin = computed(() => this.definition().range?.min ?? undefined);
  protected rangeMax = computed(() => this.definition().range?.max ?? undefined);

  protected numberValue = signal<number | null>(null);

  ngOnInit(): void {
    const initial = this.selectedValues();
    if (initial.length > 0) {
      const parsed = Number(initial[0]);
      if (!isNaN(parsed)) this.numberValue.set(parsed);
    }
  }

  protected onInput(value: number | null): void {
    this.numberValue.set(value);
    if (value != null && !isNaN(value)) {
      this.valuesChange.emit([String(value)]);
    } else {
      this.valuesChange.emit([]);
    }
  }
}
