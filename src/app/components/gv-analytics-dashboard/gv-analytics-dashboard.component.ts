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
import { Component, EventEmitter, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { Application, ApplicationService, Dashboard } from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { AnalyticsService } from '../../services/analytics.service';
import { NavRouteService } from '../../services/nav-route.service';

@Component({
  selector: 'app-gv-analytics-dashboard',
  templateUrl: './gv-analytics-dashboard.component.html',
  styleUrls: ['./gv-analytics-dashboard.component.css']
})
export class GvAnalyticsDashboardComponent implements OnInit, OnDestroy {

  @Input() dashboard: Dashboard;

  private subscription: any;
  private application: Application;
  definition: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private analyticsService: AnalyticsService,
    private navRouteService: NavRouteService,
  ) {
  }

  ngOnInit(): void {
    this.subscription = this.route.queryParams.subscribe(queryParams => {
      if (queryParams && !queryParams.skipRefresh) {
        this.refresh(queryParams);
      }
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  refresh(queryParams) {
    if (this.dashboard) {
      this.application = this.route.snapshot.data.application;
      const timeSlot = this.analyticsService.getTimeSlotFromQueryParams();

      this.definition = JSON.parse(this.dashboard.definition).map((widget) => {
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
          const selected = queryParams[widget.chart.request.field];
          if (Array.isArray(selected)) {
            widget.selected = selected;
          } else {
            widget.selected = [selected];
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
        const requestParameters = {
          ...{ applicationId: this.application.id, from: timeSlot.from, to: timeSlot.to, interval: timeSlot.interval },
          ...widget.chart.request,
          ...this.analyticsService.getQueryFromPath(widget.chart.request.field, widget.chart.request.ranges)
        };
        const itemsPromise = this.applicationService.getApplicationAnalytics(requestParameters).toPromise();

        if (widget.chart.type === 'table') {

          const style = () => 'justify-content: flex-end; text-align: right;';
          widget.chart.data[0].field = 'key';
          widget.chart.data[1].field = 'value';
          widget.chart.data[1].headerStyle = style;
          widget.chart.data[1].style = style;
          if (widget.chart.percent) {
            widget.chart.data[2].field = 'percent';
            widget.chart.data[2].headerStyle = style;
            widget.chart.data[2].style = style;
          }

          widget.items = itemsPromise.then((items: any) => {
            // @ts-ignore
            const keys = Object.keys(items.values);
            if (widget.chart.percent) {
              delete requestParameters.query;
              requestParameters.type = 'count';
              return this.applicationService.getApplicationAnalytics(requestParameters).toPromise().then(responseTotal => {
                return keys.map((key) => {
                  const value = items.values[key];
                  return {
                    id: key,
                    key: items.metadata[key].name,
                    value,
                    // @ts-ignore
                    percent: `${parseFloat(value / responseTotal.hits * 100).toFixed(2)}%`
                  };
                });
              });
            }
            return keys.map((key) => {
              return { id: key, key: items.metadata[key].name, value: items.values[key], percent: undefined };
            });
          });
        } else {
          widget.items = itemsPromise.then((items) => {
            // @ts-ignore
            const values = items.values;
            if (Array.isArray(values)) {
              values.forEach((item) => {
                const visible = queryParams[item.field];
                if (visible) {
                  item.buckets.forEach((bucket) => {
                    bucket.visible = !visible.length || visible.includes(bucket.name);
                  });
                }
              });
            }
            return items;
          });
        }
        return widget;
      });
    }
  }

  onTableSelect({ detail }) {
    const queryParams: any = {};
    queryParams[detail.options.request.field] = detail.items.map((item) => item.id || item.key);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    });
  }

  @HostListener(':gv-chart-line:zoom', ['$event.detail'])
  onChartLineZoom(e) {
    e.timeframe = null;
    this.router.navigate([], {
      queryParams: e,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    });
  }

  @HostListener(':gv-chart-line:select', ['$event.detail'])
  onChartLineSelect(e) {
    const aggs = e.request.aggs;
    if (aggs && e.value) {
      const fields = aggs.split('field:');
      if (fields && fields[1]) {
        const queryParams = {};
        const fieldValue = this.route.snapshot.queryParams[fields[1]];
        if (fieldValue) {
          const visible = !fieldValue.includes(e.value);
          if (Array.isArray(fieldValue)) {
            if (visible) {
              queryParams[fields[1]] = fieldValue.concat(e.value);
            } else {
              fieldValue.splice(fieldValue.indexOf(e.value), 1);
              queryParams[fields[1]] = [...[], ...fieldValue];
            }
          } else {
            if (visible) {
              queryParams[fields[1]] = [fieldValue, e.value];
            } else {
              queryParams[fields[1]] = null;
            }
          }
        } else {
          queryParams[fields[1]] = e.value;
        }
        this.navRouteService.navigateForceRefresh([], {
          queryParams,
          queryParamsHandling: 'merge',
          fragment: this.analyticsService.fragment,
        });
      }
    }
  }
}
