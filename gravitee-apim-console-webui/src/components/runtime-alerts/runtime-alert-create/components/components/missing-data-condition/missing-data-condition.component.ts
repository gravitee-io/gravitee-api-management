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
import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'missing-data-condition',
  styleUrls: ['../scss/conditions.component.scss'],
  standalone: false,
  template: `
    <form *ngIf="form" [formGroup]="form" class="condition-row">
      <div class="condition-row__label">
        <span class="mat-body-2">{{ label }}</span>
      </div>
      <mat-form-field class="condition-row__form-field">
        <mat-label>Duration</mat-label>
        <input matInput formControlName="duration" type="number" min="1" required />
        <mat-error *ngIf="form.controls.duration.hasError('required')">Duration is required</mat-error>
        <mat-error *ngIf="form.controls.duration.hasError('min')">Duration must be greater than 0.</mat-error>
      </mat-form-field>
      <mat-form-field class="condition-row__form-field">
        <mat-label>Time unit</mat-label>
        <mat-select formControlName="timeUnit" required>
          <mat-option *ngFor="let unit of timeUnits" [value]="unit">{{ unit }}</mat-option>
        </mat-select>
        <mat-error *ngIf="form.controls.timeUnit.hasError('required')">Time unit is required</mat-error>
      </mat-form-field>
    </form>
  `,
})
export class MissingDataConditionComponent {
  @Input({ required: true }) form: FormGroup;
  @Input() label = 'No event for';
  protected timeUnits = ['Seconds', 'Minutes', 'Hours'];
}
