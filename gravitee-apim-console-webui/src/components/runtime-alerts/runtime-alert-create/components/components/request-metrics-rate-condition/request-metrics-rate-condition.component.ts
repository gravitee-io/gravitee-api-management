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
import { FormControl, FormGroup } from '@angular/forms';

import { SimpleMetricsForm } from '../metrics-simple-condition/metrics-simple-condition.models';
import { Metrics, Scope } from '../../../../../../entities/alert';
import { ApiMetrics } from '../../../../../../entities/alerts/api.metrics';
import { AggregationFormGroup } from '../components';
import { AlertTriggerEntity } from '../../../../../../entities/alerts/alertTriggerEntity';

type MetricsRateFormGroup = FormGroup<{
  comparison: SimpleMetricsForm;
  operator: FormControl<string>;
  threshold: FormControl<number>;
  duration: FormControl<number>;
  timeUnit: FormControl<string>;
  projections: AggregationFormGroup;
}>;

@Component({
  selector: 'request-metrics-rate-condition',
  styleUrls: ['../scss/conditions.component.scss'],
  template: `
    <form *ngIf="form" [formGroup]="form">
      <div class="condition-row">
        <metrics-simple-condition
          [form]="form.controls.comparison"
          [metrics]="metrics"
          [referenceId]="referenceId"
          [referenceType]="referenceType"
          [alertToUpdateConditions]="alertToUpdate?.conditions[0]"
        ></metrics-simple-condition>
      </div>
      <div class="condition-row">
        <div class="condition-row__label">
          <span class="mat-body-2">If rate is</span>
        </div>
        <threshold-condition [form]="form" thresholdType="percentage" [updateData]="alertToUpdate?.conditions[0]"></threshold-condition>
      </div>
      <div class="condition-row">
        <missing-data-condition [form]="form" [label]="'For'" [alertToUpdate]="alertToUpdate"></missing-data-condition>
      </div>

      <aggregation-condition
        [form]="form.controls.projections"
        [properties]="properties"
        [alertToUpdate]="alertToUpdate"
      ></aggregation-condition>
    </form>
  `,
  standalone: false,
})
export class RequestMetricsRateConditionComponent {
  @Input({ required: true }) form: MetricsRateFormGroup;
  @Input({ required: true }) metrics: Metrics[];
  @Input({ required: true }) referenceId: string;
  @Input({ required: true }) set referenceType(scope: Scope) {
    this._referenceType = scope;
    this.properties = Metrics.filterByScope(ApiMetrics.METRICS, scope)?.filter(property => property.supportPropertyProjection);
  }
  get referenceType() {
    return this._referenceType;
  }
  @Input() public alertToUpdate: AlertTriggerEntity;

  protected properties: Metrics[];
  private _referenceType: Scope;
  protected readonly alert = alert;
}
