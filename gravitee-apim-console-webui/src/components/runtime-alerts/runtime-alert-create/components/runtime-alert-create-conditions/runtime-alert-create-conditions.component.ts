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

import { RuntimeAlertCreateConditionsFactory } from './runtime-alert-create-conditions.factory';

import { Metrics, Scope } from '../../../../../entities/alert';
import { Rule } from '../../../../../entities/alerts/rule.metrics';

@Component({
  selector: 'runtime-alert-create-conditions',
  templateUrl: './runtime-alert-create-conditions.component.html',
  styleUrls: ['./runtime-alert-create-conditions.component.scss'],
})
export class RuntimeAlertCreateConditionsComponent {
  @Input({ required: true }) referenceType: Scope;
  @Input({ required: true }) set rule(value: Rule) {
    if (value) {
      this.ruleType = `${value.source}@${value.type}`;
      this.conditionsForm = RuntimeAlertCreateConditionsFactory.create(this.ruleType);
      this.metrics = Metrics.filterByScope(Rule.findByScopeAndType(this.referenceType, this.ruleType).metrics, this.referenceType);
    }
  }

  protected ruleType: string;
  protected metrics: Metrics[];
  protected types: string[];
  protected conditionsForm: FormGroup;
}
