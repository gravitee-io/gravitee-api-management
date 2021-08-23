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
import { getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';

import { ITimeframe, TimeframeRanges } from '../../../components/quick-time-range/quick-time-range.component';
import { ApiService } from '../../../services/api.service';
// eslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-chart-gauge');
// eslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-chart-line');

class ApisStatusDashboardController {
  private readonly RED_COLOR = '#D9534F';
  private readonly ORANGE_COLOR = '#F0AD4E';
  private readonly GREEN_COLOR = '#5CB85C';

  private currentTimeframe: ITimeframe;

  private query: {
    from: number;
    to: number;
    interval: number;
  };
  private apisInError: number;
  private apisInWarning: number;
  private viewAllApis: boolean;
  private startedAPIsWithHC: any[];
  private displayedApis: any[];

  constructor(private apis: any[], private ApiService: ApiService, private $q) {
    'ngInject';

    this.apis = this.apis.sort((a, b) => (a.name + ' ' + a.version).localeCompare(b.name + ' ' + b.version));
    this.startedAPIsWithHC = this.apis.filter((api) => this.hasHealthcheck(api) && api.state === 'STARTED');

    this.viewAllApis = this.startedAPIsWithHC.length === this.apis.length;

    this.updateDisplayedApis();
    this.timeframeChange(TimeframeRanges.LAST_MINUTE);
  }

  updateDisplayedApis() {
    if (this.viewAllApis) {
      this.displayedApis = this.apis;
    } else {
      this.displayedApis = this.startedAPIsWithHC;
    }
  }

  timeframeChange(timeframe: ITimeframe) {
    this.currentTimeframe = timeframe;
    this.refresh();
  }

  refresh() {
    const now = Date.now();
    this.query = {
      from: now - this.currentTimeframe.range,
      to: now,
      interval: this.currentTimeframe.interval,
    };

    let refreshedApisInErrorNumber = 0;
    let refreshedApisInWarningNumber = 0;

    const apisPromises = this.startedAPIsWithHC.map((api) => {
      const promises = [
        this.ApiService.apiHealthAverage(api.id, {
          ...this.query,
          type: 'AVAILABILITY',
        }),
        this.ApiService.apiHealth(api.id, 'availability'),
      ];

      return this.$q.all(promises).then((responses) => {
        const availabilityResponse = responses[0];

        const values = availabilityResponse.data.values;
        const timestamp = availabilityResponse && availabilityResponse.data && availabilityResponse.data.timestamp;

        if (values && values.length > 0) {
          values.forEach((value) => {
            value.buckets.forEach((bucket) => {
              if (bucket) {
                const availabilitySeries = this._getAvailabilitySeries(bucket);
                const chartData = this._computeChartData(timestamp, availabilitySeries);

                api.chartData = chartData;
              } else {
                delete api.chartData;
              }
            });
          });
        }

        const healthResponse = responses[1];
        if (healthResponse.data && healthResponse.data.global) {
          api.availability = healthResponse.data.global[this.currentTimeframe.id];
          if (api.availability >= 0) {
            if (api.availability <= 80) {
              refreshedApisInErrorNumber++;
            } else if (api.availability <= 95) {
              refreshedApisInWarningNumber++;
            }
            api.uptimeSeries = this._getApiAvailabilityForGauge(api.availability);
          }
        }
      });
    });
    this.$q.all(apisPromises).then(() => {
      this.apisInError = refreshedApisInErrorNumber;
      this.apisInWarning = refreshedApisInWarningNumber;
    });
  }

  hasHealthcheck(api) {
    return api.healthcheck_enabled;
  }

  isInError(api) {
    return this.hasHealthcheck(api) && api.availability >= 0 && api.availability <= 80 && api.state === 'STARTED';
  }

  isInWarning(api) {
    return this.hasHealthcheck(api) && api.availability > 80 && api.availability <= 95 && api.state === 'STARTED';
  }

  isStopped(api) {
    return api.state === 'STOPPED';
  }

  getPictureDisplayName(api: { name: string; version: string }): string {
    return getPictureDisplayName(api);
  }

  _getApiAvailabilityForGauge(availability) {
    const averageUptime = Math.round(availability * 100) / 100;

    let color;
    if (averageUptime <= 80) {
      color = this.RED_COLOR;
    } else if (averageUptime <= 95) {
      color = this.ORANGE_COLOR;
    } else {
      color = this.GREEN_COLOR;
    }
    return [
      {
        name: 'Availability',
        data: [
          {
            color: color,
            radius: '112%',
            innerRadius: '88%',
            y: averageUptime,
          },
        ],
        dataLabels: [
          {
            enabled: true,
            align: 'center',
            verticalAlign: 'middle',
            format: '{point.y}%',
            borderWidth: 0,
            style: {
              fontSize: '12px',
            },
          },
        ],
      },
    ];
  }

  _computeChartData(timestamp, availabilitySeries) {
    return {
      plotOptions: {
        series: {
          pointStart: timestamp && timestamp.from,
          pointInterval: timestamp && timestamp.interval,
        },
      },
      series: availabilitySeries,
      legend: {
        enabled: false,
      },
      xAxis: {
        type: 'datetime',
        dateTimeLabelFormats: {
          month: '%e. %b',
          year: '%b',
        },
      },
      yAxis: [
        {
          visible: false,
          max: 100,
        },
      ],
    };
  }

  _getAvailabilitySeries(bucket) {
    return [
      {
        name: 'Availability',
        data: bucket.data,
        color: '#5CB85C',
        type: 'column',
        labelSuffix: '%',
        decimalFormat: true,
        zones: [
          {
            value: 80,
            color: this.RED_COLOR,
          },
          {
            value: 95,
            color: this.ORANGE_COLOR,
          },
          {
            color: this.GREEN_COLOR,
          },
        ],
      },
    ];
  }
}

export default ApisStatusDashboardController;
