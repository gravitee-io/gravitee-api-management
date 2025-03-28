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
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { FormControl, ValidationErrors, ValidatorFn, Validators } from "@angular/forms";
import { takeUntil, tap } from "rxjs/operators";
import { Subject } from "rxjs";

import { OPTIONAL_CONTROLS_NAMES, SimpleMetricsForm } from "./metrics-simple-condition.models";

import { Conditions, ConditionType, Metrics, Scope } from "../../../../../../entities/alert";
import { AlertTriggerEntity } from "../../../../../../entities/alerts/alertTriggerEntity";
import {
  CompareCondition, StringCondition,
  ThresholdCondition,
  ThresholdRangeCondition
} from "../../../../../../entities/alerts/conditions";

@Component({
  selector: "metrics-simple-condition",
  templateUrl: "./metrics-simple-condition.component.html",
  styleUrls: ["../scss/conditions.component.scss"]
})
export class MetricsSimpleConditionComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input({ required: true }) form: SimpleMetricsForm;
  @Input({ required: true }) metrics: Metrics[];
  @Input({ required: true }) referenceId: string;
  @Input({ required: true }) referenceType: Scope;
  @Input() public alertToUpdate: AlertTriggerEntity;

  protected types: string[];
  protected conditionType = ConditionType;
  protected metric: Metrics;


  ngOnInit() {
    this.types = this.metrics.find((metric) => metric.key === this.metrics[0].key).conditions;

    if (this.alertToUpdate) {
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
        tap((value) => {
          if (value != null) {
            this.metric = value;
            this.form.controls.type.enable();
            this.form.controls.type.reset();
            this.types = this.metrics.find((metric) => metric.key === value.key)?.conditions;
            this.removeControls(OPTIONAL_CONTROLS_NAMES);
          }
        }),
        takeUntil(this.unsubscribe$)
      )
      .subscribe();
  }


  private onTypeChanges() {
    this.form.controls.type.valueChanges
      .pipe(
        tap((typeValue) => {
          this.removeControls(OPTIONAL_CONTROLS_NAMES);
          this.addControls(typeValue);
        }),
        takeUntil(this.unsubscribe$)
      )
      .subscribe();
  }

  private addControls(typeValue: string) {
    if (ConditionType.THRESHOLD === typeValue) {
      this.form.addControl("operator", new FormControl(null, Validators.required));
      this.form.addControl("threshold", new FormControl(null, [Validators.required, Validators.min(1)]));
    } else if (ConditionType.THRESHOLD_RANGE === typeValue) {
      this.form.addControl("lowThreshold", new FormControl(null, Validators.required));
      this.form.addControl("highThreshold", new FormControl(null, [Validators.required, this.validateHighThreshold()]));
    } else if (ConditionType.COMPARE === typeValue) {
      this.form.addControl("operator", new FormControl(null, Validators.required));
      this.form.addControl("multiplier", new FormControl(null, [Validators.required, Validators.min(1)]));
      this.form.addControl("property", new FormControl(null, [Validators.required]));
    } else if (ConditionType.STRING === typeValue) {
      this.form.addControl("operator", new FormControl(null, Validators.required));
      this.form.addControl("pattern", new FormControl(null, Validators.required));
    }
  }

  private seedControls() {
    const alertToUpdateCondition = this.alertToUpdate.conditions[0] as ThresholdCondition | ThresholdRangeCondition | CompareCondition | StringCondition;
    this.setMetricControl(alertToUpdateCondition);
    this.setTypeControl(alertToUpdateCondition);

    if (ConditionType.THRESHOLD === alertToUpdateCondition.type) {
      this.form.addControl("operator", new FormControl(null, Validators.required));
      this.form.addControl("threshold", new FormControl(null, [Validators.required, Validators.min(1)]));
    } else if (ConditionType.THRESHOLD_RANGE === alertToUpdateCondition.type) {
      this.form.addControl("lowThreshold", new FormControl(alertToUpdateCondition.thresholdLow, Validators.required));
      this.form.addControl("highThreshold", new FormControl(alertToUpdateCondition.thresholdHigh, [Validators.required, this.validateHighThreshold()]));
    } else if (ConditionType.COMPARE === alertToUpdateCondition.type) {
      this.setOperatorControl(alertToUpdateCondition);
      this.form.addControl("multiplier", new FormControl(alertToUpdateCondition.multiplier, [Validators.required, Validators.min(1)]));
      this.form.addControl("property", new FormControl(null, [Validators.required]));
    } else if (ConditionType.STRING === alertToUpdateCondition.type) {
      this.setOperatorControl(alertToUpdateCondition);
      this.form.addControl("pattern", new FormControl(null, Validators.required)); // ???
    }
  }

  private setMetricControl (alertToUpdateCondition) {
    this.metric = this.metrics.find((metric ) => metric.key === alertToUpdateCondition.property);
    this.form.controls.metric.setValue(this.metric);
  }

  private setTypeControl (alertToUpdateCondition) {
    this.types = this.metrics.find((m) => m.key === this.metric.key)?.conditions;
    this.form.controls.type.setValue(alertToUpdateCondition.type);
    this.form.controls.type.enable();
  }

  private setOperatorControl (alertToUpdateCondition) {
    const condition = Conditions.findByType(alertToUpdateCondition.type);
    const operator = condition.getOperators().find(o => o.key === alertToUpdateCondition.operator);
    this.form.addControl("operator", new FormControl(operator, Validators.required));
  }

  private validateHighThreshold(): ValidatorFn {
    return (control: FormControl): ValidationErrors | null => {
      const lowThresholdValue = this.form.controls.lowThreshold?.value;
      const highThresholdValue = control.value;

      if (lowThresholdValue != null && highThresholdValue != null) {
        return lowThresholdValue < highThresholdValue ? null : { min: "High threshold must be higher than low threshold" };
      }
      return null;
    };
  }

  private removeControls(controls) {
    controls.forEach((controlKey) => {
      if (this.form.contains(controlKey)) this.form.removeControl(controlKey);
    });
  }
}
