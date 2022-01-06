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
import AnalyticsService from './analytics.service';
import * as _ from 'lodash';
import { Dashboard } from '../entities/dashboard';

// tslint:disable-next-line:interface-name
export interface AverageableField {
  label: string;
  value: string;
  type: 'duration' | 'length' | 'count';
}

class DashboardService {
  private AnalyticsService: AnalyticsService;

  constructor(private $http, private Constants, AnalyticsService: AnalyticsService) {
    'ngInject';
    this.AnalyticsService = AnalyticsService;
  }

  get(dashboardId: string) {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/dashboards/` + dashboardId);
  }

  list(referenceType: string, silent = false) {
    return this.$http.get(`${this.Constants.env.baseURL}/configuration/dashboards/` + '?reference_type=' + referenceType, {
      silentCall: silent,
    });
  }

  create(dashboard: Dashboard) {
    return this.$http.post(`${this.Constants.env.baseURL}/configuration/dashboards/`, dashboard);
  }

  update(dashboard: Dashboard) {
    return this.$http.put(`${this.Constants.env.baseURL}/configuration/dashboards/` + dashboard.id, {
      id: dashboard.id,
      reference_type: dashboard.reference_type,
      reference_id: dashboard.reference_id,
      name: dashboard.name,
      query_filter: dashboard.query_filter,
      order: dashboard.order,
      enabled: dashboard.enabled,
      definition: dashboard.definition,
    });
  }

  delete(dashboard: Dashboard) {
    return this.$http.delete(`${this.Constants.env.baseURL}/configuration/dashboards/` + dashboard.id);
  }

  getChartService() {
    return {
      chart: {
        service: {
          caller: this.AnalyticsService,
          function: this.AnalyticsService.analytics,
        },
      },
    };
  }

  getAverageableFields(): AverageableField[] {
    return [
      {
        label: 'Global latency (ms)',
        value: 'response-time',
        type: 'duration',
      },
      {
        label: 'API latency (ms)',
        value: 'api-response-time',
        type: 'duration',
      },
      {
        label: 'Proxy latency (ms)',
        value: 'proxy-latency',
        type: 'duration',
      },
      {
        label: 'Request content length (byte)',
        value: 'request-content-length',
        type: 'length',
      },
      {
        label: 'Response content length (byte)',
        value: 'response-content-length',
        type: 'length',
      },
    ];
  }

  getProjectionAggregates() {
    return [
      {
        label: 'Average',
        value: 'avg',
      },
      {
        label: 'Minimum',
        value: 'min',
      },
      {
        label: 'Maximum',
        value: 'max',
      },
    ];
  }

  getHttpStatusField() {
    return {
      label: 'HTTP Status',
      value: 'status',
    };
  }

  getIndexedFields() {
    return [
      {
        label: 'API',
        value: 'api',
      },
      {
        label: 'Application',
        value: 'application',
      },
      {
        label: 'Plan',
        value: 'plan',
      },
      {
        label: 'Path',
        value: 'path',
      },
      {
        label: 'Mapped path',
        value: 'mapped-path',
      },
      this.getHttpStatusField(),
      {
        label: 'Tenant',
        value: 'tenant',
      },
      {
        label: 'Host',
        value: 'host',
      },
      {
        label: 'Consumer IP',
        value: 'remote-address',
      },
      {
        label: 'Country',
        value: 'geoip.country_iso_code',
      },
      {
        label: 'City',
        value: 'geoip.city_name',
      },
      {
        label: 'User',
        value: 'user',
      },
      {
        label: 'User agent',
        value: 'user_agent.name',
      },
      {
        label: 'Operating system',
        value: 'user_agent.os_name',
      },
      {
        label: 'Zone',
        value: 'zone',
      },
    ];
  }

  getNumericFields() {
    return _.concat(this.getHttpStatusField(), this.getAverageableFields());
  }

  getAggregateFields() {
    return _.concat(
      _.map(this.getAverageableFields(), (field: any) => {
        field.aggLabel = 'Average ' + _.lowerCase(field.label);
        field.aggValue = 'avg:' + field.value;
        return field;
      }),
      _.map(this.getIndexedFields(), (field: any) => {
        field.aggLabel = 'By ' + _.lowerCase(field.label);
        field.aggValue = 'field:' + field.value;
        return field;
      }),
    );
  }
}

export default DashboardService;
