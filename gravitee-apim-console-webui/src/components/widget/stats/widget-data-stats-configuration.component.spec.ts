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
import { IComponentControllerService } from 'angular';

import { setupAngularJsTesting } from '../../../../jest.setup';

setupAngularJsTesting();

describe('WidgetDataStatsConfigurationComponent', () => {
  let $componentController: IComponentControllerService;
  let widgetDataStatsConfigurationComponent: any;

  beforeEach(inject((_QualityRuleService_, _$componentController_) => {
    $componentController = _$componentController_;
    widgetDataStatsConfigurationComponent = $componentController('gvWidgetDataStatsConfiguration', null, {});
    widgetDataStatsConfigurationComponent.chart = {};
    widgetDataStatsConfigurationComponent.$onInit();
  }));

  it('init chart data', () => {
    const defaultSelectedStats = [
      {
        color: '#66bb6a',
        key: 'min',
        label: 'min',
        unit: 'ms',
      },
      {
        color: '#ef5350',
        key: 'max',
        label: 'max',
        unit: 'ms',
      },
      {
        color: '#42a5f5',
        key: 'avg',
        label: 'avg',
        unit: 'ms',
      },
      {
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
        key: 'rps',
        label: 'requests per second',
      },
      {
        color: 'black',
        key: 'count',
        label: 'total',
      },
    ];

    expect(widgetDataStatsConfigurationComponent.chart).toEqual({
      request: {
        type: 'stats',
        field: 'response-time',
      },
      data: defaultSelectedStats,
    });
    expect(widgetDataStatsConfigurationComponent.selectedStatsKeys).toEqual(['min', 'max', 'avg', 'rps', 'count']);
  });

  it('set `chart` properly when selecting API latency', () => {
    widgetDataStatsConfigurationComponent.selectedField = {
      label: 'API latency (ms)',
      value: 'api-response-time',
      type: 'duration',
    };
    widgetDataStatsConfigurationComponent.onFieldChanged();

    const selectedStats = [
      {
        color: '#66bb6a',
        key: 'min',
        label: 'min',
        unit: 'ms',
      },
      {
        color: '#42a5f5',
        key: 'avg',
        label: 'avg',
        unit: 'ms',
      },
      {
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
        key: 'rps',
        label: 'requests per second',
      },
      {
        color: 'black',
        key: 'count',
        label: 'total',
      },
    ];

    widgetDataStatsConfigurationComponent.selectedStatsKeys = ['min', 'avg', 'rps', 'count'];

    widgetDataStatsConfigurationComponent.onStatsChanged();

    expect(widgetDataStatsConfigurationComponent.chart).toEqual({
      request: {
        type: 'stats',
        field: 'api-response-time',
      },
      data: selectedStats,
    });
  });

  it('set `chart` properly when selecting Request content length', () => {
    widgetDataStatsConfigurationComponent.selectedField = {
      label: 'Request content length (byte)',
      value: 'request-content-length',
      type: 'length',
    };
    widgetDataStatsConfigurationComponent.onFieldChanged();

    const selectedStats = [
      {
        color: '#66bb6a',
        key: 'min',
        label: 'min',
        unit: 'byte',
      },
      {
        color: '#ef5350',
        key: 'max',
        label: 'max',
        unit: 'byte',
      },
      {
        color: '#42a5f5',
        key: 'avg',
        label: 'avg',
        unit: 'byte',
      },
    ];

    widgetDataStatsConfigurationComponent.selectedStatsKeys = ['min', 'max', 'avg'];
    widgetDataStatsConfigurationComponent.onStatsChanged();

    expect(widgetDataStatsConfigurationComponent.chart).toEqual({
      request: {
        type: 'stats',
        field: 'request-content-length',
      },
      data: selectedStats,
    });
  });
});
