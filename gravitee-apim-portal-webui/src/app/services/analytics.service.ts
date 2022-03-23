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
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { ActivatedRoute } from '@angular/router';

import { SearchQueryParam } from '../utils/search-query-param.enum';

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  removableQueryParams = ['from', 'to', 'log', 'timestamp', ...Object.values(SearchQueryParam)];
  queryParams = ['dashboard', 'timeframe', ...Object.values(this.removableQueryParams)];
  advancedQueryParams = ['_id', 'transaction', 'method', 'uri', 'response-time', 'status', 'api', 'body'];
  timeframes: any;
  fragment = 'h';
  methods = [
    { value: '3', label: 'GET' },
    { value: '7', label: 'POST' },
    { value: '8', label: 'PUT' },
    { value: '2', label: 'DELETE' },
    { value: '6', label: 'PATCH' },
    { value: '5', label: 'OPTIONS' },
    { value: '4', label: 'HEAD' },
    { value: '9', label: 'TRACE' },
    { value: '1', label: 'CONNECT' },
    { value: '0', label: 'OTHER' },
  ];
  responseTimes = [
    { value: '[0 TO 100]', label: '0 to 100ms' },
    { value: '[100 TO 200]', label: '100 to 200ms' },
    { value: '[200 TO 300]', label: '200 to 300ms' },
    { value: '[300 TO 400]', label: '300 to 400ms' },
    { value: '[400 TO 500]', label: '400 to 500ms' },
    { value: '[500 TO 1000]', label: '500 to 1000ms' },
    { value: '[1000 TO 2000]', label: '1000 to 2000ms' },
    { value: '[2000 TO 5000]', label: '2000 to 5000ms' },
    { value: '[5000 TO *]', label: '> 5000ms' },
  ];
  httpStatus = [
    { value: '100', label: '100 - CONTINUE' },
    { value: '101', label: '101 - SWITCHING PROTOCOLS' },
    { value: '102', label: '102 - PROCESSING' },
    { value: '200', label: '200 - OK' },
    { value: '201', label: '201 - CREATED' },
    { value: '202', label: '202 - ACCEPTED' },
    { value: '203', label: '203 - NON AUTHORITATIVE INFORMATION' },
    { value: '204', label: '204 - NO CONTENT' },
    { value: '205', label: '205 - RESET CONTENT' },
    { value: '206', label: '206 - PARTIAL CONTENT' },
    { value: '207', label: '207 - MULTI STATUS' },
    { value: '300', label: '300 - MULTIPLE CHOICES' },
    { value: '301', label: '301 - MOVED PERMANENTLY' },
    { value: '302', label: '302 - FOUND' },
    { value: '303', label: '303 - SEE OTHER' },
    { value: '304', label: '304 - NOT MODIFIED' },
    { value: '305', label: '305 - USE PROXY' },
    { value: '307', label: '307 - TEMPORARY REDIRECT' },
    { value: '400', label: '400 - BAD REQUEST' },
    { value: '401', label: '401 - UNAUTHORIZED' },
    { value: '402', label: '402 - PAYMENT REQUIRED' },
    { value: '403', label: '403 - FORBIDDEN' },
    { value: '404', label: '404 - NOT FOUND' },
    { value: '405', label: '405 - METHOD NOT ALLOWED' },
    { value: '406', label: '406 - NOT ACCEPTABLE' },
    { value: '407', label: '407 - PROXY AUTHENTICATION REQUIRED' },
    { value: '408', label: '408 - REQUEST TIMEOUT' },
    { value: '409', label: '409 - CONFLICT' },
    { value: '410', label: '410 - GONE' },
    { value: '411', label: '411 - LENGTH REQUIRED' },
    { value: '412', label: '412 - PRECONDITION FAILED' },
    { value: '413', label: '413 - REQUEST ENTITY TOO LARGE' },
    { value: '414', label: '414 - REQUEST URI TOO LONG' },
    { value: '415', label: '415 - UNSUPPORTED MEDIA TYPE' },
    { value: '416', label: '416 - REQUESTED RANGE NOT SATISFIABLE' },
    { value: '417', label: '417 - EXPECTATION FAILED' },
    { value: '422', label: '422 - UNPROCESSABLE ENTITY' },
    { value: '423', label: '423 - LOCKED' },
    { value: '424', label: '424 - FAILED DEPENDENCY' },
    { value: '429', label: '429 - TOO MANY REQUESTS' },
    { value: '500', label: '500 - INTERNAL SERVER ERROR' },
    { value: '501', label: '501 - NOT IMPLEMENTED' },
    { value: '502', label: '502 - BAD GATEWAY' },
    { value: '503', label: '503 - SERVICE UNAVAILABLE' },
    { value: '504', label: '504 - GATEWAY TIMEOUT' },
    { value: '505', label: '505 - HTTP VERSION NOT SUPPORTED' },
    { value: '507', label: '507 - INSUFFICIENT STORAGE' },
  ];

  constructor(private translateService: TranslateService, private route: ActivatedRoute) {
    translateService
      .get([
        i18n('analytics.timeframes.minutes'),
        i18n('analytics.timeframes.hour'),
        i18n('analytics.timeframes.hours'),
        i18n('analytics.timeframes.day'),
        i18n('analytics.timeframes.days'),
      ])
      .toPromise()
      .then(translatedTimeframes => {
        const values = Object.values(translatedTimeframes);
        const minutes = values[0];
        const hour = values[1];
        const hours = values[2];
        const day = values[3];
        const days = values[4];
        this.timeframes = [
          {
            id: '5m',
            title: '5',
            description: minutes,
            range: 1000 * 60 * 5,
            interval: 1000 * 10,
          },
          {
            id: '30m',
            title: '30',
            description: minutes,
            range: 1000 * 60 * 30,
            interval: 1000 * 15,
          },
          {
            id: '1h',
            title: '1',
            description: hour,
            range: 1000 * 60 * 60,
            interval: 1000 * 30,
          },
          {
            id: '3h',
            title: '3',
            description: hours,
            range: 1000 * 60 * 60 * 3,
            interval: 1000 * 60,
          },
          {
            id: '6h',
            title: '6',
            description: hours,
            range: 1000 * 60 * 60 * 6,
            interval: 1000 * 60 * 2,
          },
          {
            id: '12h',
            title: '12',
            description: hours,
            range: 1000 * 60 * 60 * 12,
            interval: 1000 * 60 * 5,
          },
          {
            id: '1d',
            title: '1',
            description: day,
            range: 1000 * 60 * 60 * 24,
            interval: 1000 * 60 * 10,
          },
          {
            id: '3d',
            title: '3',
            description: days,
            range: 1000 * 60 * 60 * 24 * 3,
            interval: 1000 * 60 * 30,
          },
          {
            id: '7d',
            title: '7',
            description: days,
            range: 1000 * 60 * 60 * 24 * 7,
            interval: 1000 * 60 * 60,
          },
          {
            id: '14d',
            title: '14',
            description: days,
            range: 1000 * 60 * 60 * 24 * 14,
            interval: 1000 * 60 * 60 * 3,
          },
          {
            id: '30d',
            title: '30',
            description: days,
            range: 1000 * 60 * 60 * 24 * 30,
            interval: 1000 * 60 * 60 * 6,
          },
          {
            id: '90d',
            title: '90',
            description: days,
            range: 1000 * 60 * 60 * 24 * 90,
            interval: 1000 * 60 * 60 * 12,
          },
        ];
      });
  }

  static buildQueryParam(queryParam, q: string) {
    // use 'contains' wildcard for body parameter
    if (q === 'body') {
      queryParam = `*${this.escapeReservedCharacters(queryParam)}*`;
    }
    // use 'starts with' wildcard for uri parameter
    else if (q === 'uri') {
      queryParam = `${this.escapeReservedCharacters(queryParam)}*`;
    }
    // elsewhere, use quotes to match exact string
    else if (queryParam !== '?') {
      queryParam = '\\"' + queryParam + '\\"';
      queryParam = queryParam.replace(/\//g, '\\\\/');
    }
    return queryParam;
  }

  private static escapeReservedCharacters(paramValue: string) {
    return paramValue.replace(/(\+|-|=|&{2}|\|{2}|>|<|!|\(|\)|{|}|\[|]|\^|"|~|\?|:|\\|\/)/g, '\\\\$1');
  }

  getQueryFromPath(field?, ranges?) {
    const params = Object.keys(this.route.snapshot.queryParams)
      .filter(q => !this.queryParams.includes(q))
      .filter(q => this.route.snapshot.queryParams[q].length)
      .filter(q => (ranges && ranges.length) || !field || q !== field)
      .map(q => {
        const queryParam = this.route.snapshot.queryParams[q];
        if (typeof queryParam === 'string') {
          return q + ':' + AnalyticsService.buildQueryParam(queryParam, q);
        }
        return '(' + q + ':' + queryParam.map(qp => AnalyticsService.buildQueryParam(qp, q)).join(' OR ') + ')';
      });
    if (params && params.length) {
      return { query: params.join(' AND ') };
    }
    return {};
  }

  getTimeSlotFromQueryParams() {
    const timeframe = this.route.snapshot.queryParams.timeframe;
    let from = parseInt(this.route.snapshot.queryParams.from, 10);
    let to = parseInt(this.route.snapshot.queryParams.to, 10);

    let interval;
    const now = Date.now();
    if (from && to) {
      const diff = to - from;
      let selectedTimeframe;
      this.timeframes.forEach(t => {
        if (t.range < diff) {
          selectedTimeframe = t;
        }
      });
      if (!selectedTimeframe) {
        selectedTimeframe = this.timeframes[0];
      }
      interval = selectedTimeframe.interval;
    } else {
      let currentTimeframe = this.timeframes.find(t => t.id === timeframe);
      if (!currentTimeframe) {
        currentTimeframe = this.timeframes.find(t => t.id === '1d');
      }
      from = now - currentTimeframe.range;
      to = now;
      interval = currentTimeframe.interval;
    }
    return { from, to, interval };
  }

  getRemovableQueryParams() {
    return this.removableQueryParams.reduce((acc, val) => {
      acc[val] = null;
      return acc;
    }, {});
  }

  getDefaultStatsOptions(): Promise<Array<any>> {
    return this.translateService
      .get([
        'dashboard.stats.min',
        'dashboard.stats.max',
        'dashboard.stats.avg',
        'dashboard.stats.rps',
        'dashboard.stats.rpm',
        'dashboard.stats.rph',
        'dashboard.stats.total',
        'dashboard.stats.ms',
      ])
      .toPromise()
      .then(translated => {
        const translatedValues = Object.values(translated);
        return [
          { key: 'min', label: translatedValues[0], unit: translatedValues[7], color: '#66bb6a' },
          { key: 'max', label: translatedValues[1], unit: translatedValues[7], color: '#ef5350' },
          { key: 'avg', label: translatedValues[2], unit: translatedValues[7], color: '#42a5f5' },
          {
            key: 'rps',
            label: translatedValues[3],
            color: '#ff8f2d',
            fallback: [
              {
                key: 'rpm',
                label: translatedValues[4],
              },
              {
                key: 'rph',
                label: translatedValues[5],
              },
            ],
          },
          { key: 'count', label: translatedValues[6] },
        ];
      });
  }
}
