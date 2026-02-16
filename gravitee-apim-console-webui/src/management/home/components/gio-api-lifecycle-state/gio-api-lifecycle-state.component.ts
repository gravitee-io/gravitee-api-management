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

export type ApiLifecycleStateData = {
  values?: { [key: string]: number };
};

const LIFECYCLE_STATE_DISPLAYABLE = {
  CREATED: {
    label: 'Created',
    color: '#6978ff',
  },
  DEPRECATED: {
    label: 'Deprecated',
    color: '#bf3f0e',
  },
  PUBLISHED: {
    label: 'Published',
    color: '#02c37f',
  },
  UNPUBLISHED: {
    label: 'Unpublished',
    color: '#d3d5dc',
  },
};

@Component({
  selector: 'gio-api-lifecycle-state',
  templateUrl: './gio-api-lifecycle-state.component.html',
  standalone: false,
})
export class GioApiLifecycleStateComponent implements OnChanges {
  @Input()
  data: ApiLifecycleStateData;

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
        const status = LIFECYCLE_STATE_DISPLAYABLE[key];
        if (!status) {
          // Best effort: Ignore unknown key
          return null;
        }

        return {
          label: `${status.label}`,
          color: `${status.color}`,
          value,
        };
      })
      .filter(v => v != null);

    this.isEmpty = isEmpty(this.chartPieInput) || !this.chartPieInput.some(v => v.value > 0);
  }
}
