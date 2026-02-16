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
import { Component, Inject, Input, OnInit } from '@angular/core';
import { ControlContainer, FormGroup, FormGroupDirective } from '@angular/forms';

import { Rule } from '../../../../../entities/alerts/rule.metrics';
import { Scope } from '../../../../../entities/alert';
import { ALERT_SEVERITIES, AlertSeverity, AlertTriggerEntity } from '../../../../../entities/alerts/alertTriggerEntity';
import { Constants } from '../../../../../entities/Constants';

export type GeneralFormValue = {
  name: string;
  enabled: boolean;
  rule: Rule;
  severity: AlertSeverity;
  description: string;
};

@Component({
  standalone: false,
  selector: 'runtime-alert-create-general',
  templateUrl: './runtime-alert-create-general.component.html',
  styleUrls: ['./runtime-alert-create-general.component.scss'],
  viewProviders: [{ provide: ControlContainer, useExisting: FormGroupDirective }],
})
export class RuntimeAlertCreateGeneralComponent implements OnInit {
  @Input() public alertToUpdate: AlertTriggerEntity;
  @Input({ required: true }) set referenceType(value: Scope) {
    this.rules = Rule.findByScope(value, this.constants?.org?.settings?.cloudHosted?.enabled);
  }

  public generalForm: FormGroup;
  public rules: Rule[];
  public alertSeverities = ALERT_SEVERITIES;

  constructor(
    @Inject(Constants) private readonly constants: Constants,
    private readonly formGroupDirective: FormGroupDirective,
  ) {}

  ngOnInit() {
    this.generalForm = this.formGroupDirective.form.get('generalForm') as FormGroup;

    if (this.alertToUpdate) {
      this.seedForm();
    } else {
      this.generalForm.controls.rule.valueChanges.subscribe(value => {
        this.generalForm.patchValue({
          description: value.description,
        });
      });
    }
  }

  seedForm() {
    this.generalForm.patchValue({
      name: this.alertToUpdate.name,
      enabled: this.alertToUpdate.enabled,
      rule: this.rules.find(rule => rule.type === this.alertToUpdate.type),
      severity: this.alertToUpdate.severity,
      description: this.alertToUpdate.description,
    });
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.generalForm.disable() : this.generalForm.enable();
  }
}
