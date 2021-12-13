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

import { CompareCondition, Metrics, StringCondition, ThresholdCondition, ThresholdRangeCondition, Tuple } from '../alert';
import TenantService from '../../services/tenant.service';

const statusloader = () => {
  const events: Tuple[] = [];
  events.push(new Tuple('DOWN', 'Down'));
  events.push(new Tuple('TRANSITIONALLY_DOWN', 'Transitionally down'));
  events.push(new Tuple('TRANSITIONALLY_UP', 'Transitionally up'));
  events.push(new Tuple('UP', 'Up'));
  return events;
};

export class HealthcheckMetrics extends Metrics {
  static OLD_STATUS_NAME: HealthcheckMetrics = new HealthcheckMetrics(
    'status.old',
    'Old Status',
    [StringCondition.TYPE],
    false,
    undefined,
    statusloader,
  );

  static NEW_STATUS_NAME: HealthcheckMetrics = new HealthcheckMetrics(
    'status.new',
    'New Status',
    [StringCondition.TYPE],
    false,
    undefined,
    statusloader,
  );

  static ENDPOINT_NAME: HealthcheckMetrics = new HealthcheckMetrics('endpoint.name', 'Endpoint name', [StringCondition.TYPE], true);

  static RESPONSE_TIME: HealthcheckMetrics = new HealthcheckMetrics('response_time', 'Response Time (ms)', [
    ThresholdCondition.TYPE,
    ThresholdRangeCondition.TYPE,
    CompareCondition.TYPE,
  ]);

  static TENANT: HealthcheckMetrics = new HealthcheckMetrics(
    'tenant',
    'Tenant',
    [StringCondition.TYPE],
    false,
    undefined,
    (type: number, id: string, $injector: any) => {
      const tenants: Tuple[] = [];

      // PLATFORM: Search for all registered tenants
      ($injector.get('TenantService') as TenantService).list().then((result) => {
        result.data.forEach((tenant) => {
          tenants.push(new Tuple(tenant.id, tenant.name));
        });
      });

      return tenants;
    },
  );

  static METRICS: HealthcheckMetrics[] = [
    HealthcheckMetrics.OLD_STATUS_NAME,
    HealthcheckMetrics.NEW_STATUS_NAME,
    HealthcheckMetrics.ENDPOINT_NAME,
    HealthcheckMetrics.RESPONSE_TIME,
    HealthcheckMetrics.TENANT,
  ];
}
