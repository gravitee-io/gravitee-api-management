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

import { Metrics, Scope } from '../../../../../../entities/alert';
import { AggregationFormGroup } from '../components';
import { HealthcheckMetrics } from '../../../../../../entities/alerts/healthcheck.metrics';

type EndpointHealthCheckFormGroup = FormGroup<{
  projections: AggregationFormGroup;
}>;

@Component({
  selector: 'endpoint-health-check-condition',
  template: `
    <div>
      <p>
        This rule does not require any configuration. However, it is possible to define an aggregation to receive an alert per api endpoint
        instead of an unique alert in case multiple endpoints are concerned
      </p>
    </div>
    <aggregation-condition [form]="form.controls.projections" [properties]="properties"></aggregation-condition>
  `,
  standalone: false,
})
export class EndpointHealthCheckConditionComponent {
  @Input({ required: true }) form: EndpointHealthCheckFormGroup;
  @Input({ required: true }) set referenceType(scope: Scope) {
    this.properties = Metrics.filterByScope(HealthcheckMetrics.METRICS, scope)?.filter((property) => property.supportPropertyProjection);
  }
  protected properties: Metrics[];
}
