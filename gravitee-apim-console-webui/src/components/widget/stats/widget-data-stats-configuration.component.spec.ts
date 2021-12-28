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
    const bindings = {
      chart: {},
    };
    widgetDataStatsConfigurationComponent = $componentController('gvWidgetDataStatsConfiguration', null, bindings);
    widgetDataStatsConfigurationComponent.$onInit();
  }));

  it('init chart data', () => {
    expect(widgetDataStatsConfigurationComponent.chart).toEqual({
      request: {
        type: 'stats',
        field: 'response-time',
      },
      data: [
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
      ],
    });
    expect(widgetDataStatsConfigurationComponent.selectedStats).toEqual(['min', 'max', 'avg', 'rps', 'count']);
  });

  it('set `chart` properly when selecting API latency', () => {
    widgetDataStatsConfigurationComponent.chart.request.field = 'api-response-time';
    widgetDataStatsConfigurationComponent.selectedStats = ['min', 'avg', 'rps', 'count'];
    widgetDataStatsConfigurationComponent.onStatsChanged();

    expect(widgetDataStatsConfigurationComponent.chart).toEqual({
      request: {
        type: 'stats',
        field: 'api-response-time',
      },
      data: [
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
      ],
    });
  });
});
