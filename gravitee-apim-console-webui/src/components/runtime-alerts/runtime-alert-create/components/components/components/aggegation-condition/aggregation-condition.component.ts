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

import { Metrics } from '../../../../../../../entities/alert';
import { AlertTriggerEntity } from '../../../../../../../entities/alerts/alertTriggerEntity';

export type AggregationFormGroup = FormGroup<{
  property: FormControl;
}>;

@Component({
  selector: 'aggregation-condition',
  templateUrl: './aggregation-condition.component.html',
  styleUrls: ['./aggregation-condition.component.scss'],
  standalone: false,
})
export class AggregationConditionComponent implements OnInit {
  @Input({ required: true }) form: AggregationFormGroup;
  @Input({ required: true }) properties: Metrics[];
  @Input() public alertToUpdate: AlertTriggerEntity;

  ngOnInit() {
    if (this.alertToUpdate) {
      const property = this.properties.find(p => p.key === this.alertToUpdate.conditions[0].projections[0].property);
      this.form.controls.property.setValue(property);
    }
  }
}
