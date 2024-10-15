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

import { GioChartPieModule } from '../gio-chart-pie/gio-chart-pie.module';
import { GioChartPieInput } from '../gio-chart-pie/gio-chart-pie.component';

export type ApiAnalyticsResponseStatusRanges = {
  isLoading: boolean;
  data?: { label: string; value: number }[];
};

@Component({
  selector: 'api-analytics-response-status-ranges',
  standalone: true,
  imports: [MatCard, GioChartPieModule, GioLoaderModule],
  templateUrl: './api-analytics-response-status-ranges.component.html',
  styleUrl: './api-analytics-response-status-ranges.component.scss',
})
export class ApiAnalyticsResponseStatusRangesComponent implements OnChanges {
  @Input()
  title: string;

  @Input()
  responseStatusRanges: ApiAnalyticsResponseStatusRanges;

  inputDescription = 'Nb hits';
  totalInputDescription = 'Nb hits total';
  input: GioChartPieInput[];

  ngOnChanges(changes: SimpleChanges) {
    if (changes.responseStatusRanges && !this.responseStatusRanges?.isLoading) {
      this.input = this.responseStatusRanges?.data.map((data) => ({
        label: getLabel(data.label),
        value: data.value,
        color: getColor(data.label),
      }));
    }
  }
}

const getColor = (label: string): string => {
  if (label.startsWith('2')) {
    return '#30ab61';
  } else if (label.startsWith('3')) {
    return '#365bd3';
  } else if (label.startsWith('4')) {
    return '#ff9f40';
  } else if (label.startsWith('5')) {
    return '#cf3942';
  } else {
    return '#bbb';
  }
};

const getLabel = (label: string): string => {
  if (label.startsWith('1')) {
    return '1xx';
  } else if (label.startsWith('2')) {
    return '2xx';
  } else if (label.startsWith('3')) {
    return '3xx';
  } else if (label.startsWith('4')) {
    return '4xx';
  } else if (label.startsWith('5')) {
    return '5xx';
  } else {
    return label;
  }
};
