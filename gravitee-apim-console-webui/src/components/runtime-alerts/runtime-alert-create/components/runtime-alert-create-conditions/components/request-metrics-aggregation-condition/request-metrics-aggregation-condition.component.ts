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

import { AggregationCondition, Metrics } from '../../../../../../../entities/alert';

@Component({
  selector: 'request-metrics-aggregation-condition',
  styleUrls: ['../scss/conditions.component.scss'],
  template: `
    <form *ngIf="form" [formGroup]="form">
      <div class="condition-row">
        <div class="condition-row__label">
          <span class="mat-body-2">Calculate</span>
        </div>

        <mat-form-field class="condition-row__form-field">
          <mat-label>Function</mat-label>
          <mat-select formControlName="function" required>
            <mat-option *ngFor="let func of functions" [value]="func">{{ func.name }}</mat-option>
          </mat-select>
          <mat-error *ngIf="form.controls.function.hasError('required')">Function is required.</mat-error>
        </mat-form-field>

        <mat-form-field class="condition-row__form-field">
          <mat-label>Metric</mat-label>
          <mat-select formControlName="metric" required>
            <mat-option *ngFor="let metric of metrics" [value]="metric">{{ metric.name }}</mat-option>
          </mat-select>
          <mat-error *ngIf="form.controls.metric.hasError('required')">Metric is required.</mat-error>
        </mat-form-field>
      </div>

      <div class="condition-row">
        <div class="condition-row__label">
          <span class="mat-body-2">If result is</span>
        </div>
        <threshold-condition [form]="form"></threshold-condition>
      </div>

      <div class="condition-row">
        <missing-data-condition [form]="form" [label]="'For'"></missing-data-condition>
      </div>
    </form>
  `,
})
export class RequestMetricsAggregationConditionComponent {
  @Input({ required: true }) form: FormGroup;
  @Input({ required: true }) metrics: Metrics[];
  protected functions = AggregationCondition.FUNCTIONS;
  protected operators = AggregationCondition.OPERATORS;
  protected timeUnits = ['Seconds', 'Minutes', 'Hours'];
}
