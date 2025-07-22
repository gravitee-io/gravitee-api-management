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

import { Component, input } from '@angular/core';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

import { PieChartWidgetComponent } from './components/pie-chart-widget/pie-chart-widget.component';
import { LineChartWidgetComponent } from './components/line-chart-widget/line-chart-widget.component';

import { ChartWidgetConfig } from '../../../../../../entities/management-api-v2/analytics/analytics';
import { GioChartPieModule } from '../../../../../../shared/components/gio-chart-pie/gio-chart-pie.module';

@Component({
  selector: 'chart-widget',
  imports: [
    PieChartWidgetComponent,
    LineChartWidgetComponent,
    GioChartPieModule,
    GioLoaderModule,
    MatCardModule,
    MatIcon,
    MatTooltip,
    MatIcon,
  ],
  templateUrl: './chart-widget.component.html',
  styleUrl: './chart-widget.component.scss',
})
export class ChartWidgetComponent {
  public config = input<ChartWidgetConfig>();
}
