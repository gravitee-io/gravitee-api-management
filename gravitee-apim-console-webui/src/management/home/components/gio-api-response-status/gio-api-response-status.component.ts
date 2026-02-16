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
import { isEmpty } from 'lodash';

import { GioChartPieInput } from '../../../../shared/components/gio-chart-pie/gio-chart-pie.component';

export type ApiResponseStatusData = {
  values?: { [key: string]: number };
  metadata?: { [key: string]: { name: string; order: string } };
};

const STATUS_DISPLAYABLE = {
  '100.0-200.0': {
    order: 1,
    label: '1xx',
    color: '#d3d5dc',
  },
  '200.0-300.0': {
    order: 2,
    label: '2xx',
    color: '#02c37f',
  },
  '300.0-400.0': {
    order: 3,
    label: '3xx',
    color: '#6978ff',
  },
  '400.0-500.0': {
    order: 4,
    label: '4xx',
    color: '#bf3f0e',
  },
  '500.0-600.0': {
    order: 5,
    label: '5xx',
    color: '#cb0366',
  },
};

@Component({
  selector: 'gio-api-response-status',
  templateUrl: './gio-api-response-status.component.html',
  standalone: false,
})
export class GioApiResponseStatusComponent implements OnChanges {
  @Input()
  data: ApiResponseStatusData;

  chartPieInput: GioChartPieInput[];
  isEmpty = true;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.data) {
      this.buildDataSource();
    }
  }

  private buildDataSource() {
    this.chartPieInput = Object.entries(this.data?.values ?? {})
      .map(([key, value]) => {
        const status = STATUS_DISPLAYABLE[key];
        if (!status) {
          // Best effort: Ignore unknown key
          return null;
        }

        return {
          label: `${status.label}`,
          color: `${status.color}`,
          order: status.order,
          value,
        };
      })
      .filter(v => v != null)
      .sort((a, b) => a.order - b.order);

    this.isEmpty = isEmpty(this.chartPieInput) || !this.chartPieInput.some(v => v.value > 0);
  }
}
