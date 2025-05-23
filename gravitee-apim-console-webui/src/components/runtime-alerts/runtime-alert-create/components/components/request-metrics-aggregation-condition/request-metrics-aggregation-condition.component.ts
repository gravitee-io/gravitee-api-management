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
import { FormControl, FormGroup } from '@angular/forms';

import { AggregationCondition, Metrics, Scope } from '../../../../../../entities/alert';
import { AggregationFormGroup } from '../components';
import { ApiMetrics } from '../../../../../../entities/alerts/api.metrics';
import { AlertTriggerEntity } from '../../../../../../entities/alerts/alertTriggerEntity';
import { AlertsAggregationCondition } from '../../../../../../entities/alerts/conditions';

type RequestMetricsAggregationFormGroup = FormGroup<{
  metric: FormControl<Metrics>;
  function: FormControl;
  operator: FormControl;
  threshold: FormControl<number>;
  duration: FormControl<number>;
  timeUnit: FormControl<string>;
  projections: AggregationFormGroup;
}>;

@Component({
  selector: 'request-metrics-aggregation-condition',
  styleUrls: ['../scss/conditions.component.scss'],
  standalone: false,
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

      <aggregation-condition [form]="form.controls.projections" [properties]="properties" [alertToUpdate]="alertToUpdate">
      </aggregation-condition>
    </form>
  `,
})
export class RequestMetricsAggregationConditionComponent implements OnInit {
  @Input({ required: true }) form: RequestMetricsAggregationFormGroup;
  @Input({ required: true }) metrics: Metrics[];
  @Input({ required: true }) set referenceType(scope: Scope) {
    this.properties = Metrics.filterByScope(ApiMetrics.METRICS, scope)?.filter((property) => property.supportPropertyProjection);
  }
  @Input() public alertToUpdate: AlertTriggerEntity;

  protected functions = AggregationCondition.FUNCTIONS;
  protected operators = AggregationCondition.OPERATORS;
  protected timeUnits = ['Seconds', 'Minutes', 'Hours'];
  protected properties: Metrics[];

  ngOnInit() {
    if (this.alertToUpdate) {
      this.seedForm();
    }
  }

  seedForm() {
    const conditions = this.alertToUpdate.conditions[0] as AlertsAggregationCondition;
    const metric = this.metrics.find((m) => m.key === conditions.property) || null;
    const conditionsFunction = this.functions.find((f) => f.key === conditions.function) || null;
    const operator = this.operators.find((o) => o.key === conditions.operator) || null;
    const timeUnit = this.timeUnits.find((t) => t.toLowerCase() === conditions.timeUnit.toLowerCase()) || null;

    let projectionProperty = null;
    if (conditions.projections?.length) {
      const projection = conditions.projections[0];
      projectionProperty = this.properties?.find((p) => p.key === projection?.property) || null;
    }

    this.form.patchValue({
      function: conditionsFunction,
      metric,
      operator,
      threshold: conditions.threshold,
      duration: conditions.duration,
      timeUnit,
      projections: {
        property: projectionProperty,
      },
    });
  }
}
