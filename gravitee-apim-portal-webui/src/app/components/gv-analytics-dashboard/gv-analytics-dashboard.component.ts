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
import { Component, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Application, ApplicationService, Dashboard } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ActivatedRoute, Router } from '@angular/router';
import { AnalyticsService } from '../../services/analytics.service';
import { NavRouteService } from '../../services/nav-route.service';

import '@gravitee/ui-components/wc/gv-table';

@Component({
  selector: 'app-gv-analytics-dashboard',
  templateUrl: './gv-analytics-dashboard.component.html',
  styleUrls: ['./gv-analytics-dashboard.component.css'],
})
export class GvAnalyticsDashboardComponent implements OnInit, OnDestroy {
  @Input() dashboard: Dashboard;
  @Output() searching = new EventEmitter<boolean>();

  private subscription: any;
  application: Application;
  definition: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private analyticsService: AnalyticsService,
    private navRouteService: NavRouteService,
  ) {}

  hasLinkToAlert(title: string): boolean {
    return title === 'Response times' || title === 'Response Status';
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

      const promises: Promise<any>[] = [];
      this.definition = JSON.parse(this.dashboard.definition).map(widget => {
        if (widget.chart.request.ranges) {
          widget.chart.request.ranges = widget.chart.request.ranges.replace(/%3B/g, ';');
        }
        // table
        if (widget.chart.columns) {
          widget.chart.selectable = 'multi';
          widget.chart.data = widget.chart.columns.map(column => {
            return { label: column };
          });
          const selected = queryParams[widget.chart.request.field];
          if (Array.isArray(selected)) {
            widget.selected = selected;
          } else {
            widget.selected = [selected];
          }
          if (widget.chart.percent) {
            widget.chart.data.push({ label: '%' });
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
          ...this.analyticsService.getQueryFromPath(widget.chart.request.field, widget.chart.request.ranges),
        };

        if (widget.chart.percent) {
          const percentPromises: Promise<any>[] = [];
          percentPromises.push(this.applicationService.getApplicationAnalytics(requestParameters).toPromise());
          delete requestParameters.query;
          requestParameters.type = 'count';
          percentPromises.push(this.applicationService.getApplicationAnalytics(requestParameters).toPromise());
          promises.push(Promise.all(percentPromises));
        } else {
          promises.push(this.applicationService.getApplicationAnalytics(requestParameters).toPromise());
        }
        return widget;
      });

      this.searching.emit(true);
      Promise.all(promises)
        .then((response: any) => {
          this.definition.forEach((widget, i) => {
            let items;
            let itemsPercent;
            if (Array.isArray(response[i])) {
              items = response[i][0];
              itemsPercent = response[i][1];
            } else {
              items = response[i];
            }
            if (widget.chart.type === 'table') {
              const style = () => 'justify-content: flex-end; text-align: right;';
              widget.chart.data[0].field = 'key';
              widget.chart.data[0].width = 'fit-content(90%)';
              widget.chart.data[0].style = () =>
                '--gv-table-cell--d: block; height: auto; text-overflow: ellipsis; white-space: nowrap; overflow: hidden;';
              widget.chart.data[1].field = 'value';
              widget.chart.data[1].headerStyle = style;
              widget.chart.data[1].style = style;
              widget.chart.data[1].width = 'auto';
              if (itemsPercent) {
                widget.chart.data[2].field = 'percent';
                widget.chart.data[2].headerStyle = style;
                widget.chart.data[2].style = style;
                widget.chart.data[2].width = 'auto';
              }

              const keys = Object.keys(items.values);
              if (itemsPercent) {
                widget.items = keys.map(key => {
                  const value = items.values[key];
                  return {
                    id: key,
                    key: items.metadata[key].name,
                    value,
                    percent: `${parseFloat((value / itemsPercent.hits) * 100 + '').toFixed(2)}%`,
                  };
                });
              } else {
                widget.items = keys.map(key => {
                  return { id: key, key: items.metadata[key].name, value: items.values[key], percent: undefined };
                });
              }
            } else {
              const values = items.values;
              if (Array.isArray(values)) {
                values.forEach(item => {
                  const visible = queryParams[item.field];
                  if (visible) {
                    item.buckets.forEach(bucket => {
                      bucket.visible = !visible.length || visible.includes(bucket.name);
                    });
                  }
                });
              }
              widget.items = items;
            }
          });
        })
        .finally(() => this.searching.emit(false));
    }
  }

  onTableSelect({ detail }) {
    const queryParams: any = {};
    queryParams[detail.options.request.field] = detail.items.map(item => item.id || item.key);
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment,
    });
  }

  @HostListener(':gv-chart-line:zoom', ['$event.detail'])
  onChartLineZoom(e) {
    e.timeframe = null;
    this.router.navigate([], {
      queryParams: e,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment,
    });
  }

  @HostListener(':gv-chart-line:select', ['$event.detail.options'])
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
