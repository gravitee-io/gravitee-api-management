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
import type * as Highcharts from 'highcharts';

const PARAGRAPH_FONT_FAMILY = 'Manrope, sans-serif';
const HEADER_FONT_FAMILY = 'Kanit, Roboto, Helvetica Neue, sans-serif';

/**
 * Options the Console sets once on the Highcharts instance shared by its Angular and AngularJS charts.
 *
 * No `palette` here: `setOptions` merges it into Highcharts' own palette defaults, so a chart asking for
 * the same palette no longer differs from them and gets its color variables declared under `:root`.
 * The `gv-chart-*` web components draw inside a shadow root, which `:root` never reaches: their
 * background falls back to black. Each chart pins its palette in its own options instead.
 */
export const CONSOLE_HIGHCHARTS_OPTIONS: Highcharts.Options = {
  chart: {
    style: {
      fontFamily: PARAGRAPH_FONT_FAMILY,
    },
  },
  title: {
    style: {
      fontFamily: HEADER_FONT_FAMILY,
    },
  },
  subtitle: {
    style: {
      fontFamily: PARAGRAPH_FONT_FAMILY,
    },
  },
  xAxis: {
    labels: {
      style: {
        fontFamily: PARAGRAPH_FONT_FAMILY,
      },
    },
    title: {
      style: {
        fontFamily: PARAGRAPH_FONT_FAMILY,
      },
    },
  },
  yAxis: {
    labels: {
      style: {
        fontFamily: PARAGRAPH_FONT_FAMILY,
      },
    },
    title: {
      style: {
        fontFamily: PARAGRAPH_FONT_FAMILY,
      },
    },
  },
  legend: {
    itemStyle: {
      fontFamily: PARAGRAPH_FONT_FAMILY,
    },
    itemHoverStyle: {
      fontFamily: PARAGRAPH_FONT_FAMILY,
    },
  },
  tooltip: {
    style: {
      fontFamily: PARAGRAPH_FONT_FAMILY,
    },
  },
  plotOptions: {
    series: {
      dataLabels: {
        style: {
          fontFamily: PARAGRAPH_FONT_FAMILY,
        },
      },
    },
  },
};
