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
  selector: 'threshold-range-condition',
  styleUrls: ['../../scss/conditions.component.scss'],
  template: `
    <form *ngIf="form" [formGroup]="form" class="condition-row">
      <mat-form-field class="condition-row__form-field">
        <mat-label>Low threshold</mat-label>
        <input matInput formControlName="lowThreshold" type="number" min="1" required />
        <mat-error *ngIf="form.controls.lowThreshold.hasError('required')">Low threshold is required.</mat-error>
      </mat-form-field>
      <mat-form-field class="condition-row__form-field">
        <mat-label>High threshold</mat-label>
        <input matInput formControlName="highThreshold" type="number" [min]="form.controls.lowThreshold.getRawValue()" required />
        <mat-error *ngIf="form.controls.highThreshold.hasError('required')">High threshold is required.</mat-error>
        <mat-error *ngIf="form.controls.highThreshold.hasError('min')">High threshold must be higher than low threshold.</mat-error>
      </mat-form-field>
    </form>
  `,
  standalone: false,
})
export class ThresholdRangeConditionComponent {
  @Input({ required: true }) form: FormGroup;
}
