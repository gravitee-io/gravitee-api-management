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
import { Component, OnInit, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

import { GioTimeframeWidgetModule } from '../../../../../../shared/components/gio-timeframe-widget/gio-timeframe-widget.module';

// Wrapper component that delegates to shared widget

export interface ApiAnalyticsProxyFilters {
  period: string;
  from?: number | null;
  to?: number | null;
}

@Component({
  selector: 'api-analytics-proxy-filter-bar',
  imports: [CommonModule, GioTimeframeWidgetModule],
  templateUrl: './api-analytics-proxy-filter-bar.component.html',
  styleUrl: './api-analytics-proxy-filter-bar.component.scss',
})
export class ApiAnalyticsProxyFilterBarComponent implements OnInit {
  activeFilters = input.required<ApiAnalyticsProxyFilters>();
  filtersChange = output<ApiAnalyticsProxyFilters>();
  refresh = output<void>();

  ngOnInit(): void {}
}
