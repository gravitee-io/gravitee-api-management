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
import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

@Component({
  selector: 'gd-string-value-input',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatFormField, MatLabel, MatInput],
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="gd-value-input">
      <mat-label>Filter value</mat-label>
      <input matInput placeholder="Enter a value" [ngModel]="textValue()" (ngModelChange)="onInput($event)" />
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
export class StringValueInputComponent implements OnInit {
  selectedValues = input<string[]>([]);
  valuesChange = output<string[]>();

  protected textValue = signal('');

  ngOnInit(): void {
    const initial = this.selectedValues();
    if (initial.length > 0) this.textValue.set(initial[0]);
  }

  protected onInput(value: string): void {
    this.textValue.set(value);
    if (value.trim()) {
      this.valuesChange.emit([value.trim()]);
    } else {
      this.valuesChange.emit([]);
    }
  }
}
