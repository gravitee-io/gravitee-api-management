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
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { OPTIONAL_CONTROLS_NAMES, SimpleMetricsForm } from './metrics-simple-condition.models';

import { Conditions, ConditionType, Metrics, Scope } from '../../../../../../entities/alert';
import { AlertCondition } from '../../../../../../entities/alerts/conditions';

@Component({
  selector: 'metrics-simple-condition',
  templateUrl: './metrics-simple-condition.component.html',
  styleUrls: ['../scss/conditions.component.scss'],
  standalone: false,
})
export class MetricsSimpleConditionComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input({ required: true }) form: SimpleMetricsForm;
  @Input({ required: true }) metrics: Metrics[];
  @Input({ required: true }) referenceId: string;
  @Input({ required: true }) referenceType: Scope;
  @Input() public alertToUpdateConditions: AlertCondition;

  protected types: string[];
  protected conditionType = ConditionType;
  protected metric: Metrics;

  public updateData: AlertCondition;

  ngOnInit() {
    this.types = this.metrics.find(metric => metric.key === this.metrics[0]?.key).conditions;

    if (this.alertToUpdateConditions) {
      this.updateData = this.alertToUpdateConditions;
      this.seedControls();
    }

    this.onMetricsChanges();
    this.onTypeChanges();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private onMetricsChanges() {
    this.form.controls.metric.valueChanges
      .pipe(
        tap(value => {
          if (value != null) {
            this.metric = value;
            this.form.controls.type.enable();
            this.form.controls.type.reset();
            this.types = this.metrics.find(metric => metric.key === value?.key)?.conditions;
            this.removeControls(OPTIONAL_CONTROLS_NAMES);
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private onTypeChanges() {
    this.form.controls.type.valueChanges
      .pipe(
        tap(typeValue => {
          this.removeControls(OPTIONAL_CONTROLS_NAMES);
          this.addControls(typeValue);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private addControls(typeValue: string) {
    if (ConditionType.THRESHOLD === typeValue) {
      this.form.addControl('operator', new FormControl(null, Validators.required));
      this.form.addControl('threshold', new FormControl(null, [Validators.required, Validators.min(1)]));
    } else if (ConditionType.THRESHOLD_RANGE === typeValue) {
      this.form.addControl('lowThreshold', new FormControl(null, Validators.required));
      this.form.addControl('highThreshold', new FormControl(null, [Validators.required, this.validateHighThreshold()]));
    } else if (ConditionType.COMPARE === typeValue) {
      this.form.addControl('operator', new FormControl(null, Validators.required));
      this.form.addControl('multiplier', new FormControl(null, [Validators.required, Validators.min(1)]));
      this.form.addControl('property', new FormControl(null, [Validators.required]));
    } else if (ConditionType.STRING === typeValue) {
      this.form.addControl('operator', new FormControl(null, Validators.required));
      this.form.addControl('pattern', new FormControl(null, Validators.required));
    }
  }

  private seedControls() {
    this.setMetricControl(this.updateData);
    this.setTypeControl(this.updateData);

    if (this.updateData.type === ConditionType.THRESHOLD) {
      this.form.addControl('operator', new FormControl(null, Validators.required));
      this.form.addControl('threshold', new FormControl(null, [Validators.required, Validators.min(1)]));
    } else if (this.updateData.type === ConditionType.THRESHOLD_RANGE) {
      this.form.addControl('lowThreshold', new FormControl(this.updateData.thresholdLow, Validators.required));
      this.form.addControl(
        'highThreshold',
        new FormControl(this.updateData.thresholdHigh, [Validators.required, this.validateHighThreshold()]),
      );
    } else if (this.updateData.type === ConditionType.COMPARE) {
      this.setOperatorControl();
      this.form.addControl('multiplier', new FormControl(this.updateData.multiplier, [Validators.required, Validators.min(1)]));
      this.form.addControl('property', new FormControl(null, [Validators.required]));
    } else if (this.updateData.type === ConditionType.STRING) {
      this.setOperatorControl();
      this.setPatternControl();
    } else if (this.updateData.type === ConditionType.RATE && 'comparison' in this.updateData) {
      this.updateData = this.updateData.comparison as AlertCondition;
      if (this.updateData.type === ConditionType.THRESHOLD) {
        this.form.addControl('operator', new FormControl(null, Validators.required));
        this.form.addControl('threshold', new FormControl(null, [Validators.required, Validators.min(1)]));
        return;
      } else if (this.updateData.type === ConditionType.THRESHOLD_RANGE) {
        this.form.addControl('lowThreshold', new FormControl(this.updateData.thresholdLow, Validators.required));
        this.form.addControl(
          'highThreshold',
          new FormControl(this.updateData.thresholdHigh, [Validators.required, this.validateHighThreshold()]),
        );
      } else if (this.updateData.type === ConditionType.COMPARE) {
        this.form.addControl('operator', new FormControl(null, Validators.required));
        this.form.addControl('multiplier', new FormControl(null, [Validators.required, Validators.min(1)]));
        this.form.addControl('property', new FormControl(null, [Validators.required]));
      } else if (this.updateData.type === ConditionType.STRING) {
        this.setOperatorControl();
        this.setPatternControl();
      }
    }
  }

  private setPatternControl() {
    let pattern = '';
    if ('pattern' in this.updateData) {
      pattern = this.updateData.pattern;
    }
    this.form.addControl('pattern', new FormControl(pattern, Validators.required));
  }

  private setMetricControl(alertToUpdateCondition: AlertCondition) {
    if ('property' in alertToUpdateCondition) {
      this.metric = this.metrics.find(metric => metric?.key === alertToUpdateCondition.property);
    }
    if ('comparison' in alertToUpdateCondition) {
      this.metric = this.metrics.find(metric => metric.key === alertToUpdateCondition.comparison.property);
    }
    this.form.controls.metric.setValue(this.metric);
  }

  private setTypeControl(alertToUpdateCondition: AlertCondition) {
    this.types = this.metrics.find(m => m?.key === this.metric?.key)?.conditions;
    let type: string;
    if ('comparison' in alertToUpdateCondition) {
      type = alertToUpdateCondition.comparison.type;
    } else {
      type = alertToUpdateCondition.type;
    }

    this.form.controls.type.setValue(type);
    this.form.controls.type.enable();
  }

  private setOperatorControl() {
    const condition = Conditions.findByType(this.updateData.type);
    const operator = condition.getOperators().find(o => {
      return o?.key === this.updateData.operator;
    });
    this.form.addControl('operator', new FormControl(operator, Validators.required));
  }

  private validateHighThreshold(): ValidatorFn {
    return (control: FormControl): ValidationErrors | null => {
      const lowThresholdValue = this.form.controls.lowThreshold?.value;
      const highThresholdValue = control.value;

      if (lowThresholdValue != null && highThresholdValue != null) {
        return lowThresholdValue < highThresholdValue ? null : { min: 'High threshold must be higher than low threshold' };
      }
      return null;
    };
  }

  private removeControls(controls) {
    controls.forEach(controlKey => {
      if (this.form.contains(controlKey)) this.form.removeControl(controlKey);
    });
  }
}
