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
import * as Highcharts from 'highcharts';

import * as fs from 'fs';
import * as path from 'path';

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
  const documentRootClasses = document.documentElement.className;

  const linkChart = (type: string) => {
    const scope: ChartScope = {
      type,
      height: 180,
      width: 600,
      $watch: (_expression, listener) => (displayOptions = listener),
      $on: () => {},
    };

    const directive = new ChartDirective() as unknown as LinkableDirective;
    directive.link(scope, angular.element(chartHost), {}, { $window: window });
  };

  const applyApplicationShellRootClasses = () => {
    const shell = new DOMParser().parseFromString(fs.readFileSync(path.resolve(__dirname, '../../../../index.html'), 'utf8'), 'text/html');
    document.documentElement.className = shell.documentElement.className;
  };

  beforeEach(() => {
    jest.useFakeTimers();

    widgetCard = document.createElement('div');
    const chartWrapper = document.createElement('div');
    chartHost = document.createElement('div');
    chartWrapper.appendChild(chartHost);
    widgetCard.appendChild(chartWrapper);
    document.body.appendChild(widgetCard);
  });

  afterEach(() => {
    Highcharts.charts.filter(chart => chart && widgetCard.contains(chart.container)).forEach(chart => chart.destroy());
    widgetCard.remove();
    document.documentElement.className = documentRootClasses;
    jest.useRealTimers();
  });

  it('should render chart with the light color scheme, not the one of the browser or OS', () => {
    linkChart('areaspline');

    displayOptions({ series: [{ name: 'Availability', data: [100, 80, 100] }] });
    jest.runOnlyPendingTimers();

    const renderedChart = chartHost.querySelector<HTMLElement>('.highcharts-container');
    expect(renderedChart).not.toBeNull();
    expect(renderedChart.style.getPropertyValue('color-scheme')).toBe('light');
  });

  it('should render the sparkline tooltip, which Highcharts appends to the body, inside the light color scheme root', () => {
    applyApplicationShellRootClasses();
    linkChart('sparkline');

    displayOptions({ series: [{ name: 'Hits', data: [10, 40, 20, 70] }] });
    jest.runOnlyPendingTimers();

    const chart = Highcharts.charts.find(candidate => candidate && chartHost.contains(candidate.container));
    // jsdom lays nothing out, so a pointer event cannot open the tooltip; asking for its label is
    // what creates the container Highcharts appends to the body.
    chart.tooltip.getLabel();

    const tooltipContainer = document.body.querySelector<HTMLElement>('.highcharts-tooltip-container');
    expect(tooltipContainer).not.toBeNull();
    // The palette pins the color scheme on the chart container only, and this container is a
    // sibling of it under the body. Highcharts' own stylesheet gives it `color-scheme: light dark`
    // and overrides that with `light` under a `.highcharts-light` ancestor; jsdom computes no
    // `color-scheme`, so the test asserts the ancestor that rule needs.
    expect(tooltipContainer.closest('.highcharts-light')).toBe(document.documentElement);
  });
});
