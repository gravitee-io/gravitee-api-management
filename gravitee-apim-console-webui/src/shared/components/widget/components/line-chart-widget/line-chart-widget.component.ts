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
import { Component, input, OnChanges, SimpleChanges } from "@angular/core";
import { MatCardModule } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';

import { GioChartLineModule } from "../../../gio-chart-line/gio-chart-line.module";
import { GioChartLineData, GioChartLineOptions } from "../../../gio-chart-line/gio-chart-line.component";
import { WidgetConfig } from "../../../../../entities/management-api-v2/analytics/analytics";


export const namesFormatted = {
  'avg_gateway-response-time-ms': 'Gateway Response Time',
  'avg_endpoint-response-time-ms': 'Endpoint Response Time',
};

@Component({
  selector: 'line-chart-widget',
  imports: [MatCardModule, GioChartLineModule, GioLoaderModule],
  templateUrl: './line-chart-widget.component.html',
  styleUrl: './line-chart-widget.component.scss',
})
export class LineChartWidgetComponent implements OnChanges {
  public chartInput: GioChartLineData[];
  public chartOptions: GioChartLineOptions;
  public config = input<WidgetConfig>();

  ngOnChanges(changes: SimpleChanges) {
    this.chartInput = changes.config.currentValue.data;
    this.chartOptions = changes.config.currentValue.chartOptions;
  }
}
