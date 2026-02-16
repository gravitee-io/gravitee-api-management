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
import { Component, Input, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';

import { Conditions, Operator, Scope, Tuple } from '../../../../../../../entities/alert';
import { RuntimeAlertCreateService } from '../../../services/runtime-alert-create.service';
import { AlertCondition } from '../../../../../../../entities/alerts/conditions';

@Component({
  selector: 'string-condition',
  styleUrls: ['../../scss/conditions.component.scss', './string-condition.component.scss'],
  template: `
    <form *ngIf="form" [formGroup]="form" class="condition-row">
      <mat-form-field *ngIf="operators" class="condition-row__form-field">
        <mat-label>Operator</mat-label>
        <mat-select formControlName="operator" required>
          <mat-option *ngFor="let operator of operators" [value]="operator">{{ operator.name }}</mat-option>
        </mat-select>
        <mat-error *ngIf="form.controls.operator.hasError('required')">Operator is required.</mat-error>
      </mat-form-field>

      <mat-form-field
        class="condition-row__form-field"
        *ngIf="['EQUALS', 'NOT_EQUALS'].includes(this.form.controls.operator.getRawValue()?.key); else templateBlock"
      >
        <mat-label>Reference value</mat-label>
        <mat-select formControlName="pattern" required panelClass="reference-form-field">
          <mat-option *ngFor="let reference of references" [value]="reference">{{ reference.value }}</mat-option>
        </mat-select>
        <mat-error *ngIf="form.controls.pattern.hasError('required')">Reference value is required.</mat-error>
      </mat-form-field>

      <ng-template #templateBlock>
        <mat-form-field class="condition-row__form-field">
          <mat-label>Pattern</mat-label>
          <input matInput formControlName="pattern" required />
          <mat-error *ngIf="form.controls.pattern.hasError('required')">Pattern value is required.</mat-error>
        </mat-form-field>
      </ng-template>
    </form>
  `,
  encapsulation: ViewEncapsulation.None,
  standalone: false,
})
export class StringConditionComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input({ required: true }) form: FormGroup;
  @Input({ required: true }) referenceType: Scope;
  @Input({ required: true }) referenceId: string;
  @Input({ required: true }) metric: string;
  @Input() updateData: AlertCondition;

  protected references: Tuple[];
  protected operators: Operator[];

  constructor(private readonly createConditionsService: RuntimeAlertCreateService) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  ngOnInit() {
    const condition = Conditions.findByType(this.form.controls.type.value);
    if (condition !== undefined) {
      this.operators = condition.getOperators();
    }

    this.createConditionsService.loadDataFromMetric(this.metric, this.referenceType, this.referenceId).subscribe(references => {
      this.references = references;
    });

    this.form.controls.operator.valueChanges
      .pipe(
        tap(() => this.form.controls.pattern.reset()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
