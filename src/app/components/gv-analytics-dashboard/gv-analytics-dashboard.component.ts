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
import { Application, ApplicationService, Dashboard } from '@gravitee/ng-portal-webclient';
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
    this.definition = JSON.parse(this.dashboard.definition);
    const timeSlot = this.analyticsService.getTimeSlotFromQueryParams();

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
        const selected = this.route.snapshot.queryParams[widget.chart.request.field];
        if (typeof selected === 'string') {
          widget.selected = [selected];
        } else {
          widget.selected = selected;
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
                pointStart: timeSlot.from,
                pointInterval: timeSlot.interval,
              });
            });
          } else {
            widget.chart.data.push({
              pointStart: timeSlot.from,
              pointInterval: timeSlot.interval,
            });
          }
        }
      }
      widget.items = this.applicationService.getApplicationAnalytics({
        ...{ applicationId: this.application.id, from: timeSlot.from, to: timeSlot.to, interval: timeSlot.interval },
        ...widget.chart.request,
        ...this.analyticsService.getQueryFromPath()
      }).toPromise();
    });
  }

  onTableSelect(e) {
    const queryParams: any = {};
    queryParams[e.detail.options.request.field] = e.detail.items.map((item) => item.id || item.key);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
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
      fragment: this.analyticsService.fragment
    }).then(() => {
      this.refreshFilters.emit();
      this.refresh();
    });
  }

  @HostListener(':gv-chart-line:select', ['$event.detail'])
  onChartLineSelect(e) {
    const aggs = e.request.aggs;
    if (aggs && e.value) {
      const fields = aggs.split('field:');
      if (fields && fields[1]) {
        const queryParams = {};
        queryParams[fields[1]] = e.value;
        this.router.navigate([], {
          queryParams,
          queryParamsHandling: 'merge',
          fragment: this.analyticsService.fragment
        }).then(() => {
          this.refreshFilters.emit();
          this.refresh();
        });
      }
    }
  }
}
