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
import * as Highcharts from 'highcharts';
import 'highcharts/modules/map';
import '@gravitee/ui-components/wc/gv-chart-bar';
import '@gravitee/ui-components/wc/gv-chart-line';
import '@gravitee/ui-components/wc/gv-chart-map';
import '@gravitee/ui-components/wc/gv-chart-pie';

import { CONSOLE_HIGHCHARTS_OPTIONS } from './console-highcharts-options';

describe('CONSOLE_HIGHCHARTS_OPTIONS', () => {
  let chartHost: HTMLElement;

  beforeAll(() => {
    Highcharts.setOptions(CONSOLE_HIGHCHARTS_OPTIONS);
  });

  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    Highcharts.charts.filter(chart => chart && chartHost.shadowRoot.contains(chart.container)).forEach(chart => chart.destroy());
    chartHost.remove();
    jest.useRealTimers();
  });

  // The inputs each gv-chart-* web component the Console uses needs to draw a chart.
  const chartInputs = {
    'gv-chart-pie': {
      options: {
        name: 'Status',
        data: [
          { name: '2xx', color: '#1bc98e' },
          { name: '5xx', color: '#e64759' },
        ],
      },
      series: { values: { '200.0-300.0': 80, '500.0-600.0': 20 } },
    },
    'gv-chart-line': {
      options: {},
      series: {
        values: [
          {
            name: 'Hits',
            data: [
              [1700000000000, 10],
              [1700000060000, 20],
            ],
          },
        ],
      },
    },
    'gv-chart-map': {
      options: { name: 'Hits by country' },
      series: { values: { fr: 10, us: 20 } },
    },
    'gv-chart-bar': {
      options: { name: 'Alerts', data: [{ name: 'CRITICAL' }, { name: 'INFO' }] },
      series: { values: { CRITICAL: 3, INFO: 5 } },
    },
  };

  const renderChart = async (tagName: keyof typeof chartInputs): Promise<ShadowRoot> => {
    chartHost = document.createElement(tagName);
    chartHost.setAttribute('options', JSON.stringify(chartInputs[tagName].options));
    chartHost.setAttribute('series', JSON.stringify(chartInputs[tagName].series));
    document.body.appendChild(chartHost);

    // Lit renders and resolves the series in promises, then draws the chart in a zero-delay timeout.
    // Only zero-delay timers run: the component's 500 ms skeleton timer would delay the chart further.
    for (let attempt = 0; attempt < 20 && !chartHost.shadowRoot?.querySelector('.highcharts-background'); attempt++) {
      await Promise.resolve();
      jest.advanceTimersByTime(0);
    }
    return chartHost.shadowRoot;
  };

  it.each(Object.keys(chartInputs) as (keyof typeof chartInputs)[])('should paint the background of %s white, not black', async tagName => {
    const shadowRoot = await renderChart(tagName);

    const background = shadowRoot.querySelector('.highcharts-background');
    expect(background).not.toBeNull();
    const backgroundFill = background.getAttribute('fill');
    const backgroundVariable = /^var\((--[\w-]+)\)$/.exec(backgroundFill)?.[1];
    expect(backgroundVariable).toEqual('--highcharts-background-color');

    // jsdom resolves no CSS variable: the test looks for the rule that declares it and checks it
    // applies to an element of the chart, inside the shadow root. A rule under `:root` matches
    // nothing there, and the browser falls back to black.
    const declaringRules = Array.from(shadowRoot.querySelectorAll<HTMLStyleElement>('style.highcharts-palette'))
      .flatMap(style => Array.from(style.sheet.cssRules))
      .filter((rule): rule is CSSStyleRule => rule instanceof CSSStyleRule && rule.style.getPropertyValue(backgroundVariable) !== '');
    const reachingRules = declaringRules.filter(rule => background.closest(rule.selectorText) !== null);

    expect(reachingRules.map(rule => rule.style.getPropertyValue(backgroundVariable).trim())).toContain('#ffffff');
  });
});
