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

import { AggregationCondition, Conditions, ConditionType, Metrics } from '../../../../../../../entities/alert';
import { AlertCondition } from '../../../../../../../entities/alerts/conditions';

@Component({
  selector: 'compare-condition',
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
      <mat-form-field class="condition-row__form-field">
        <mat-label>Multiplier</mat-label>
        <input matInput formControlName="multiplier" type="number" [min]="1" required />
        <mat-error *ngIf="form.controls.multiplier.hasError('required')">Multiplier is required.</mat-error>
        <mat-error *ngIf="form.controls.multiplier.hasError('min')">The multiplier value should be greater or equals to 1.</mat-error>
      </mat-form-field>
      <mat-form-field class="condition-row__form-field">
        <mat-label>Property</mat-label>
        <mat-select formControlName="property" required>
          <mat-option *ngFor="let property of properties" [value]="property">{{ property.name }}</mat-option>
        </mat-select>
        <mat-error *ngIf="form.controls.property.hasError('required')">Property is required.</mat-error>
      </mat-form-field>
    </form>
  `,
})
export class CompareConditionComponent implements OnInit {
  @Input({ required: true }) form: FormGroup;
  @Input({ required: true }) metrics: Metrics[];

  protected operators = AggregationCondition.OPERATORS;
  protected properties: Metrics[];

  @Input() updateData: AlertCondition;

  ngOnInit() {
    const condition = Conditions.findByType(this.form.controls.type.value);
    if (condition !== undefined) {
      this.operators = condition.getOperators();
    }

    this.properties = this.metrics.filter(
      metric => metric.conditions.includes(ConditionType.COMPARE) && metric.key !== this.form.controls.metric.value.key,
    );

    if (this.updateData) {
      if (this.updateData.type === ConditionType.RATE) {
        const property = this.properties.find(p => p.key === this.updateData.property2);
        const operator = this.operators.find(p => p.key === this.updateData.operator);

        this.form.controls.property.setValue(property);
        this.form.controls.operator.setValue(operator);
        this.form.controls.multiplier.setValue(this.updateData.multiplier);
      } else {
        if ('property2' in this.updateData) {
          const property = this.properties.find(p => p.key === this.updateData.property2);
          this.form.controls.property.setValue(property);
        }
      }
    }
  }
}
