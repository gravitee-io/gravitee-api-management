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
import { TableWidgetComponent } from './components/table-widget/table-widget.component';

import { GioChartPieModule } from '../gio-chart-pie/gio-chart-pie.module';
import { WidgetConfig } from '../../../entities/management-api-v2/analytics/analytics';
import { GioChartLineModule } from '../gio-chart-line/gio-chart-line.module';

@Component({
  selector: 'widget',
  imports: [
    PieChartWidgetComponent,
    GioChartPieModule,
    GioLoaderModule,
    MatCardModule,
    MatIcon,
    MatTooltip,
    MatIcon,
    TableWidgetComponent,
    GioChartLineModule,
  ],
  templateUrl: './widget.component.html',
  styleUrl: './widget.component.scss',
})
export class WidgetComponent {
  public config = input<WidgetConfig>();
}
