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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';

import { Metrics, Scope } from '../../../../../entities/alert';
import { Rule } from '../../../../../entities/alerts/rule.metrics';
import { ApiMetrics } from '../../../../../entities/alerts/api.metrics';
import { HealthcheckMetrics } from '../../../../../entities/alerts/healthcheck.metrics';
import { AlertCondition } from '../../../../../entities/alerts/conditions';

@Component({
  selector: 'runtime-alert-create-filters',
  templateUrl: './runtime-alert-create-filters.component.html',
  styleUrls: ['./runtime-alert-create-filters.component.scss'],
  standalone: false,
})
export class RuntimeAlertCreateFiltersComponent implements OnDestroy, OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  @Input() public form;

  @Input() public alertToUpdateFilters: AlertCondition[];
  @Input({ required: true }) referenceType: Scope;
  @Input({ required: true }) referenceId: string;
  @Input({ required: true }) set rule(value: Rule) {
    this.selectedRule = value;

    this.removeAllControls();
    if (value) {
      // Metrics are depending on the source of the trigger
      if (value.source === 'REQUEST') {
        this.metrics = Metrics.filterByScope(ApiMetrics.METRICS, this.referenceType);
      } else if (value.source === 'ENDPOINT_HEALTH_CHECK') {
        this.metrics = Metrics.filterByScope(HealthcheckMetrics.METRICS, this.referenceType);
      }
    }
  }

  protected selectedRule: Rule;
  protected metrics: Metrics[];
  protected types: string[];

  ngOnInit() {
    if (this.alertToUpdateFilters) {
      this.alertToUpdateFilters.forEach(_ => {
        this.addControl();
      });
    }
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addControl() {
    this.form.push(
      new FormGroup({
        metric: new FormControl<Metrics>(null, [Validators.required]),
        type: new FormControl<string>({ value: null, disabled: true }, [Validators.required]),
      }),
    );
  }

  removeControl(index: number) {
    this.form.removeAt(index);
    this.form.markAsDirty();
  }

  private removeAllControls() {
    if (!this.form) {
      return;
    }

    while (this.form?.length !== 0) {
      this.form.removeAt(0);
    }
  }
}
