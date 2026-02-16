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
import { Component, Input, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { AggregationCondition } from '../../../../../../../entities/alert';
import { AlertCondition } from '../../../../../../../entities/alerts/conditions';

@Component({
  selector: 'threshold-condition',
  styleUrls: ['../../scss/conditions.component.scss'],
  standalone: false,
  template: `
    <form *ngIf="form" [formGroup]="form" class="condition-row">
      <mat-form-field class="condition-row__form-field">
        <mat-label>Operator</mat-label>
        <mat-select formControlName="operator" required>
          <mat-option *ngFor="let operator of operators" [value]="operator">{{ operator.name }}</mat-option>
        </mat-select>
        <mat-error *ngIf="form.controls.operator.hasError('required')">Operator is required.</mat-error>
      </mat-form-field>

      <mat-form-field *ngIf="thresholdType === 'number'; else percentageBlock" class="condition-row__form-field">
        <mat-label>Threshold</mat-label>
        <input matInput formControlName="threshold" type="number" min="1" required />
        <mat-error *ngIf="form.controls.threshold.hasError('required')">Threshold is required.</mat-error>
        <mat-error *ngIf="form.controls.threshold.hasError('min')">The threshold value should be greater or equals to 1</mat-error>
      </mat-form-field>

      <ng-template #percentageBlock>
        <mat-form-field class="condition-row__form-field">
          <mat-label>Threshold (%)</mat-label>
          <input matInput formControlName="threshold" type="number" min="1" max="100" required />
          <mat-error *ngIf="form.controls.threshold.hasError('required')">Threshold is required.</mat-error>
          <mat-error *ngIf="form.controls.threshold.hasError('min')">The threshold value should be greater or equals to 1</mat-error>
          <mat-error *ngIf="form.controls.threshold.hasError('max')">The threshold value should be not greater than 100</mat-error>
        </mat-form-field>
      </ng-template>
    </form>
  `,
})
export class ThresholdConditionComponent implements OnInit {
  @Input({ required: true }) form: FormGroup;
  @Input() thresholdType: 'number' | 'percentage' = 'number';
  @Input() updateData: AlertCondition;

  protected operators = AggregationCondition.OPERATORS;

  ngOnInit() {
    if (this.updateData) {
      this.form.controls.operator.setValue(this.operators.find(o => o.key === this.updateData.operator));
      this.form.controls.threshold.setValue(this.updateData.threshold);
    }
  }
}
