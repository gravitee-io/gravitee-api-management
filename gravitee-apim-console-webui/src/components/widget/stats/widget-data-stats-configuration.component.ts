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

import { merge } from 'lodash';
import { IOnInit } from 'angular';

import DashboardService, { AverageableField } from '../../../services/dashboard.service';

interface Stat {
  key: string;
  label: string;
  unit?: string;
  color: string;
  fallback?: any;
}

class WidgetDataStatsConfigurationController implements IOnInit {
  public chart: {
    request: { type: string; field: any };
    data: Stat[];
  };

  fields = this.DashboardService.getAverageableFields();

  selectedField: AverageableField;
  selectedStatsKeys: string[] = [];
  availableStats: Stat[];

  constructor(private readonly DashboardService: DashboardService) {
    'ngInject';
  }

  $onInit(): void {
    if (!this.chart.data) {
      const defaultField = this.fields[0];
      merge(this.chart, {
        request: {
          type: 'stats',
          field: defaultField.value,
        },
        data: WidgetDataStatsConfigurationController.getStatsAccordingToFieldType(defaultField.type),
      });
    }

    this.selectedField = this.fields.find((field) => field.value === this.chart.request.field);
    this.availableStats = WidgetDataStatsConfigurationController.getStatsAccordingToFieldType(this.selectedField.type);
    this.selectedStatsKeys = this.chart.data.map((stat) => stat.key);
  }

  onFieldChanged() {
    this.chart.request.field = this.selectedField.value;
    this.availableStats = WidgetDataStatsConfigurationController.getStatsAccordingToFieldType(this.selectedField.type);
    this.selectedStatsKeys = this.availableStats.map((stat) => stat.key);
    this.onStatsChanged();
  }

  onStatsChanged() {
    this.chart.data = this.availableStats.filter((stat) => (this.selectedStatsKeys ?? []).includes(stat.key));
  }

  private static getStatsAccordingToFieldType(fieldType: AverageableField['type']) {
    if (fieldType === 'duration') {
      return [
        {
          key: 'min',
          label: 'min',
          unit: 'ms',
          color: '#66bb6a',
        },
        {
          key: 'max',
          label: 'max',
          unit: 'ms',
          color: '#ef5350',
        },
        {
          key: 'avg',
          label: 'avg',
          unit: 'ms',
          color: '#42a5f5',
        },
        {
          key: 'rps',
          label: 'requests per second',
          color: '#ff8f2d',
          fallback: [
            {
              key: 'rpm',
              label: 'requests per minute',
            },
            {
              key: 'rph',
              label: 'requests per hour',
            },
          ],
        },
        {
          key: 'count',
          label: 'total',
          color: 'black',
        },
      ];
    } else if (fieldType === 'length') {
      return [
        {
          key: 'min',
          label: 'min',
          unit: 'byte',
          color: '#66bb6a',
        },
        {
          key: 'max',
          label: 'max',
          unit: 'byte',
          color: '#ef5350',
        },
        {
          key: 'avg',
          label: 'avg',
          unit: 'byte',
          color: '#42a5f5',
        },
      ];
    } else {
      return [];
    }
  }
}

const WidgetDataStatsConfigurationComponent: ng.IComponentOptions = {
  template: require('./widget-data-stats-configuration.html'),
  bindings: {
    chart: '<',
  },
  controller: WidgetDataStatsConfigurationController,
};

export default WidgetDataStatsConfigurationComponent;
