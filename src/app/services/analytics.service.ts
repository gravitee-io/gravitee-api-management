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

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {

  queryParams = ['timeframe', 'from', 'to', 'dashboard'];
  timeframes: any;

  constructor(private translateService: TranslateService) {
    translateService.get([i18n('analytics.timeframes.minutes'), i18n('analytics.timeframes.hours'), i18n('analytics.timeframes.days')]).toPromise()
      .then(translatedTimeframes => {
        const values = Object.values(translatedTimeframes);
        const minutes = values[0];
        const hours = values[1];
        const days = values[2];
        this.timeframes = [
          {
            id: '5m',
            title: '5',
            description: minutes,
            range: 1000 * 60 * 5,
            interval: 1000 * 10,
          }, {
            id: '30m',
            title: '30',
            description: minutes,
            range: 1000 * 60 * 30,
            interval: 1000 * 15,
          }, {
            id: '1h',
            title: '1',
            description: hours,
            range: 1000 * 60 * 60,
            interval: 1000 * 30,
          }, {
            id: '3h',
            title: '3',
            description: hours,
            range: 1000 * 60 * 60 * 3,
            interval: 1000 * 60,
          }, {
            id: '6h',
            title: '6',
            description: hours,
            range: 1000 * 60 * 60 * 6,
            interval: 1000 * 60 * 2,
          }, {
            id: '12h',
            title: '12',
            description: hours,
            range: 1000 * 60 * 60 * 12,
            interval: 1000 * 60 * 5,
          }, {
            id: '1d',
            title: '1',
            description: days,
            range: 1000 * 60 * 60 * 24,
            interval: 1000 * 60 * 10,
          }, {
            id: '3d',
            title: '3',
            description: days,
            range: 1000 * 60 * 60 * 24 * 3,
            interval: 1000 * 60 * 30,
          }, {
            id: '7d',
            title: '7',
            description: days,
            range: 1000 * 60 * 60 * 24 * 7,
            interval: 1000 * 60 * 60,
          }, {
            id: '14d',
            title: '14',
            description: days,
            range: 1000 * 60 * 60 * 24 * 14,
            interval: 1000 * 60 * 60 * 3,
          }, {
            id: '30d',
            title: '30',
            description: days,
            range: 1000 * 60 * 60 * 24 * 30,
            interval: 1000 * 60 * 60 * 6,
          }, {
            id: '90d',
            title: '90',
            description: days,
            range: 1000 * 60 * 60 * 24 * 90,
            interval: 1000 * 60 * 60 * 12,
          }
        ];
    });
  }
}
