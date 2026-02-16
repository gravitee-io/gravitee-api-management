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
import { Component, Inject, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlContainer, FormControl, FormGroup, FormGroupDirective, Validators } from '@angular/forms';

import { Metrics, Scope } from '../../../../../entities/alert';
import { Rule } from '../../../../../entities/alerts/rule.metrics';
import { AlertTriggerEntity } from '../../../../../entities/alerts/alertTriggerEntity';
import { Constants } from '../../../../../entities/Constants';

@Component({
  standalone: false,
  selector: 'runtime-alert-create-conditions',
  templateUrl: './runtime-alert-create-conditions.component.html',
  styleUrls: ['./runtime-alert-create-conditions.component.scss'],
  viewProviders: [{ provide: ControlContainer, useExisting: FormGroupDirective }],
})
export class RuntimeAlertCreateConditionsComponent implements OnInit, OnChanges {
  @Input() public referenceType: Scope;
  @Input() public referenceId: string;
  @Input() public rule: Rule;
  @Input() public alertToUpdate: AlertTriggerEntity;

  public ruleType: string;
  public metrics: Metrics[];
  public types: string[];
  public conditionsForm: FormGroup;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly formGroupDirective: FormGroupDirective,
  ) {}

  ngOnInit() {
    this.conditionsForm = this.formGroupDirective.form?.get('conditionsForm') as FormGroup;
  }

  ngOnChanges(changes: SimpleChanges) {
    this.rule = changes?.rule?.currentValue;
    if (this.rule) {
      this.ruleType = `${this.rule.source}@${this.rule.type}`;
      const cloudHostedEnabled = this.constants?.org?.settings?.cloudHosted?.enabled;
      this.metrics = Metrics.filterByScope(
        Rule.findByScopeAndType(this.referenceType, this.ruleType, cloudHostedEnabled)?.metrics ?? [],
        this.referenceType,
      );
      this.createForm(this.ruleType);
    }
  }

  createForm(rule: string) {
    this.resetForm();

    if (rule.endsWith('@MISSING_DATA')) {
      this.conditionsForm.addControl('duration', new FormControl<number>(null, [Validators.required, Validators.min(1)]));
      this.conditionsForm.addControl('timeUnit', new FormControl<string>(null, [Validators.required]));
      this.conditionsForm.addControl('type', new FormControl<string>('MISSING_DATA', [Validators.required]));
      return;
    }

    switch (rule) {
      case 'REQUEST@METRICS_SIMPLE_CONDITION':
        this.conditionsForm.addControl('metric', new FormControl<Metrics>(null, [Validators.required]));
        this.conditionsForm.addControl(
          'type',
          new FormControl<string>(
            {
              value: null,
              disabled: true,
            },
            [Validators.required],
          ),
        );
        break;

      case 'REQUEST@METRICS_AGGREGATION':
        this.conditionsForm.addControl('metric', new FormControl<Metrics>(null, [Validators.required]));
        this.conditionsForm.addControl('type', new FormControl<string>('AGGREGATION', [Validators.required]));
        this.conditionsForm.addControl('function', new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl('operator', new FormControl(null, [Validators.required]));
        this.conditionsForm.addControl('threshold', new FormControl<number>(null, [Validators.required]));
        this.conditionsForm.addControl('duration', new FormControl<number>(null, [Validators.required, Validators.min(1)]));
        this.conditionsForm.addControl('timeUnit', new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl('projections', new FormGroup({ property: new FormControl(null) }));
        break;

      case 'REQUEST@METRICS_RATE':
        this.conditionsForm.addControl(
          'comparison',
          new FormGroup({
            metric: new FormControl<Metrics>(null, [Validators.required]),
            type: new FormControl<string>({ value: null, disabled: true }, [Validators.required]),
          }),
        );
        this.conditionsForm.addControl('operator', new FormControl(null, [Validators.required]));
        this.conditionsForm.addControl('threshold', new FormControl<number>(null, [Validators.required]));
        this.conditionsForm.addControl(
          'duration',
          new FormControl<number>(null, [Validators.required, Validators.min(1), Validators.max(100)]),
        );
        this.conditionsForm.addControl('timeUnit', new FormControl<string>(null, [Validators.required]));
        this.conditionsForm.addControl(
          'projections',
          new FormGroup({
            property: new FormControl<string>(null),
          }),
        );
        break;

      case 'ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED':
        this.conditionsForm.addControl(
          'projections',
          new FormGroup({
            property: new FormControl<string>(null),
          }),
        );
        // migration todo, why and where do we use this ?
        this.conditionsForm.addControl('type', new FormControl<string>('API_HC_ENDPOINT_STATUS_CHANGED', [Validators.required]));
        break;
    }
  }

  public resetForm() {
    Object.keys(this.conditionsForm.getRawValue()).forEach(key => {
      this.conditionsForm.removeControl(key);
    });
  }
}
