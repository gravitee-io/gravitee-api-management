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
import { Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import { Api, Application, ApplicationService, Dashboard } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-gv-analytics-dashboard',
  templateUrl: './gv-analytics-dashboard.component.html',
  styleUrls: ['./gv-analytics-dashboard.component.css']
})
export class GvAnalyticsDashboardComponent {

  @Input() dashboard: Dashboard;
  @Output() refreshFilters: EventEmitter<any> = new EventEmitter();

  private application: Application;
  definition: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private analyticsService: AnalyticsService,
    ) { }

  refresh() {
    this.application = this.route.snapshot.data.application;

    const timeframe = this.route.snapshot.queryParams.timeframe;
    let from = parseInt(this.route.snapshot.queryParams.from, 10);
    let to = parseInt(this.route.snapshot.queryParams.to, 10);
    this.definition = JSON.parse(this.dashboard.definition);

    let interval;
    const now = Date.now();
    if (timeframe && !(from && to)) {
      let currentTimeframe = this.analyticsService.timeframes.find((t) => t.id === timeframe);
      if (!currentTimeframe) {
        currentTimeframe = '1d';
      }
      from = now - currentTimeframe.range;
      to = now;
      interval = currentTimeframe.interval;
    } else {
      const diff = to - from;
      let selectedTimeframe;
      this.analyticsService.timeframes.forEach((t) => {
        if (t.range < diff) {
          selectedTimeframe = t;
        }
      });
      if (!selectedTimeframe) {
        selectedTimeframe = this.analyticsService.timeframes[0];
      }
      interval = selectedTimeframe.interval;
    }

    this.definition.forEach((widget) => {
      if (widget.chart.request.ranges) {
        widget.chart.request.ranges = widget.chart.request.ranges.replace(/%3B/g, ';');
      }
      // table
      if (widget.chart.columns) {
        widget.chart.selectable = 'multi';
        widget.chart.data = widget.chart.columns.map((column) => {
          return { label: column };
        });
        if (widget.chart.percent) {
          widget.chart.data.push({ label: '%' });
        }
        const selectedKeys = this.route.snapshot.queryParams[widget.chart.request.field];
        if (typeof selectedKeys === 'string') {
          widget.selectedKeys = [selectedKeys];
        } else {
          widget.selectedKeys = selectedKeys;
        }
      } else {
        if (!widget.chart.data) {
          widget.chart.excludedKeys = ['Unknown'];
          widget.chart.data = [];
          if (widget.chart.labels) {
            widget.chart.labels.forEach((label, i) => {
              widget.chart.data.push({
                name: label,
                color: widget.chart.colors ? widget.chart.colors[i] : '',
                labelPrefix: label,
                pointStart: from,
                pointInterval: interval,
              });
            });
          } else {
            widget.chart.data.push({
              pointStart: from,
              pointInterval: interval,
            });
          }
        }
      }
      widget.items = this.applicationService.getApplicationAnalytics({
        ...{ applicationId: this.application.id, from, to, interval },
        ...widget.chart.request,
        ...this.getQueryFromPath(widget.chart.request.field)
      }).toPromise();
    });
  }

  getQueryFromPath(field) {
    const params = Object.keys(this.route.snapshot.queryParams)
      .filter((q) => q !== field && !this.analyticsService.queryParams.includes(q))
      .filter(q => this.route.snapshot.queryParams[q].length)
      .map((q) => {
        const queryParam = this.route.snapshot.queryParams[q];
        if (typeof queryParam === 'string') {
          return q + ':' + queryParam;
        }
        return '(' + q + ':\\"' + queryParam.join('\\" OR \\"') + '\\")';
    });
    if (params && params.length) {
      return { query: params.join(' AND ') };
    }
    return {};
  }

  onTableSelect(e) {
    const queryParams: any = {};
    queryParams[e.detail.options.request.field] = e.detail.items.map((item) => item.id || item.key);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: 'dashboard'
    }).then(() => {
      this.refreshFilters.emit();
      this.refresh();
    });
  }

  @HostListener(':gv-chart-line:zoom', ['$event.detail'])
  onChartLineZoom(e) {
    e.timeframe = null;
    this.router.navigate([], {
      queryParams: e,
      queryParamsHandling: 'merge',
      fragment: 'dashboard'
    }).then(() => {
      this.refreshFilters.emit();
      this.refresh();
    });
  }
}
