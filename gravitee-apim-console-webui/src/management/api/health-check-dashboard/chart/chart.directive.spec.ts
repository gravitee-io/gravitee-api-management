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
import angular from 'angular';

import ChartDirective from './chart.directive';

interface ChartScope {
  type: string;
  height: number;
  width: number;
  $watch: (expression: string, listener: (newOptions: unknown) => void) => void;
  $on: (event: string, listener: () => void) => void;
}

interface LinkableDirective {
  link: (scope: ChartScope, element: ng.IAugmentedJQuery, attributes: unknown, controller: { $window: Window }) => void;
}

describe('ChartDirective', () => {
  let chartHost: HTMLElement;
  let widgetCard: HTMLElement;
  let displayOptions: (options: unknown) => void;

  beforeEach(() => {
    jest.useFakeTimers();

    widgetCard = document.createElement('div');
    const chartWrapper = document.createElement('div');
    chartHost = document.createElement('div');
    chartWrapper.appendChild(chartHost);
    widgetCard.appendChild(chartWrapper);
    document.body.appendChild(widgetCard);

    const scope: ChartScope = {
      type: 'areaspline',
      height: 180,
      width: 600,
      $watch: (_expression, listener) => (displayOptions = listener),
      $on: () => {},
    };

    const directive = new ChartDirective() as unknown as LinkableDirective;
    directive.link(scope, angular.element(chartHost), {}, { $window: window });
  });

  afterEach(() => {
    widgetCard.remove();
    jest.useRealTimers();
  });

  it('should render chart with the light color scheme, not the one of the browser or OS', () => {
    displayOptions({ series: [{ name: 'Availability', data: [100, 80, 100] }] });
    jest.runOnlyPendingTimers();

    const renderedChart = chartHost.querySelector<HTMLElement>('.highcharts-container');
    expect(renderedChart).not.toBeNull();
    expect(renderedChart.style.getPropertyValue('color-scheme')).toBe('light');
  });
});
