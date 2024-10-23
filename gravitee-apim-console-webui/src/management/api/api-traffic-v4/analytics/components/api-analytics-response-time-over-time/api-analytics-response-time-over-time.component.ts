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
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { MatCard } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';

import { GioChartLineModule } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.module';
import { GioChartLineData, GioChartLineOptions } from '../../../../../../shared/components/gio-chart-line/gio-chart-line.component';

export type Loading = Record<string, never>;
export const ResponseTimeIsLoading: Loading = {}

export type ApiAnalyticsResponseTimeOverTime = {
  timeRange: { from: number; to: number; interval: number };
  data: number[];
};

export type ApiAnalyticsResponseTimeOverTimeComponentInput = Loading | ApiAnalyticsResponseTimeOverTime;

@Component({
  selector: 'api-analytics-response-time-over-time',
  standalone: true,
  imports: [MatCard, GioChartLineModule, GioLoaderModule],
  templateUrl: './api-analytics-response-time-over-time.component.html',
  styleUrl: './api-analytics-response-time-over-time.component.scss',
})
export class ApiAnalyticsResponseTimeOverTimeComponent implements OnChanges {
  @Input()
  title: string;

  @Input()
  responseTimeOverTime: ApiAnalyticsResponseTimeOverTimeComponentInput;

  input: GioChartLineData[];
  options: GioChartLineOptions;

  ngOnChanges(changes: SimpleChanges) {
    if (changes.responseTimeOverTime && isResponse(this.responseTimeOverTime)) {
      this.input = [
        {
          name: 'Response time (ms)',
          values: this.responseTimeOverTime.data
        }
      ];
      this.options = {
        pointStart: this.responseTimeOverTime.timeRange.from,
        pointInterval: this.responseTimeOverTime.timeRange.interval,
      };
    }
  }
}

function isResponse(input: ApiAnalyticsResponseTimeOverTimeComponentInput): input is ApiAnalyticsResponseTimeOverTime {
  return (input as ApiAnalyticsResponseTimeOverTime).data?.length > 0;
}
