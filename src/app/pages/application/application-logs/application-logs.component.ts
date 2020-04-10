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
import { Component, HostListener, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import '@gravitee/ui-components/wc/gv-chart-line';
import '@gravitee/ui-components/wc/gv-chart-pie';
import '@gravitee/ui-components/wc/gv-chart-map';
import '@gravitee/ui-components/wc/gv-stats';
import { GvAnalyticsFiltersComponent } from '../../../components/gv-analytics-filters/gv-analytics-filters.component';
import { ApplicationService, Log, Subscription } from '@gravitee/ng-portal-webclient';
import { AnalyticsService } from '../../../services/analytics.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { SearchQueryParam, SearchRequestParams } from '../../../utils/search-query-param.enum';
import { ConfigurationService } from '../../../services/configuration.service';

@Component({
  selector: 'app-application-logs',
  templateUrl: './application-logs.component.html',
  styleUrls: ['./application-logs.component.css']
})
export class ApplicationLogsComponent implements OnInit {

  logs: Array<Log>;
  log: Log;
  selectedLog: Log;
  options: any;
  format: any;
  paginationData: any = {};
  pageSizes: Array<any>;
  size: number;
  requestHeaders: Array<any>;
  responseHeaders: Array<any>;

  @ViewChild(GvAnalyticsFiltersComponent)
  filtersComponent: GvAnalyticsFiltersComponent;

  constructor(
    private applicationService: ApplicationService,
    private analyticsService: AnalyticsService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private config: ConfigurationService,
    ) {}

  ngOnInit(): void {
    this.pageSizes = this.config.get('pagination.size.values');
    this.size = this.route.snapshot.queryParams[SearchQueryParam.SIZE] ?
      parseInt(this.route.snapshot.queryParams[SearchQueryParam.SIZE], 10) : this.config.get('pagination.size.default');
  }

  refresh() {
    const timeSlot = this.analyticsService.getTimeSlotFromQueryParams();
    let field = this.route.snapshot.queryParams[SearchQueryParam.FIELD] || '@timestamp';
    field = field === 'timestamp' ? '@timestamp' : field;
    field = field === 'responseTime' ? 'response-time' : field;
    this.applicationService.getApplicationLogs({
      applicationId: this.route.snapshot.data.application.id,
      from: timeSlot.from, to: timeSlot.to,
      size: this.size,
      page: this.route.snapshot.queryParams[SearchQueryParam.PAGE] || 1,
      field,
      order: this.route.snapshot.queryParams[SearchQueryParam.ORDER] || 'DESC',
      query: this.analyticsService.getQueryFromPath().query
    }).toPromise().then(response => {
      this.logs = response.data;
      this.selectedLog = this.logs.find(l => l.id === this.route.snapshot.queryParams.log);
      const metadata = response.metadata;
      this.buildPaginationData(response.metadata.data.total);
      this.format = (key) => this.translateService.get(key).toPromise();
      this.options = {
        selectable: true,
        data: [
          { field: 'timestamp', type: 'datetime', label: i18n('application.logs.date'), style: 'color: #40A9FF', width: '200px' },
          { tag: 'status', label: i18n('application.logs.status'),
            style: ({ status }) => {
            const color = this.getStatusColor(status);
            return `--gv-tag--bdc: ${color}; --gv-tag--c: ${color};`;
            } },
          { field: 'api', label: i18n('application.logs.api'), format: (item) => metadata[item].name },
          { field: 'plan', label: i18n('application.logs.plan'), format: (item) => metadata[item].name },
          { field: 'method', label: i18n('application.logs.method'), format: (item) => item.toUpperCase(),
            style: ({ method }) => 'color:' + this.getMethodColor(method) },
          { field: 'path', label: i18n('application.logs.path'), width: '350px' },
          { field: 'responseTime', label: i18n('application.logs.responseTime'), headerStyle: () => 'justify-content: flex-end',
            format: (item) => item + ' ms', style: () => 'text-align: right' },
        ]
      };
      if (this.route.snapshot.queryParams.log && this.route.snapshot.queryParams.timestamp) {
        this.onSelectLog({ id: this.route.snapshot.queryParams.log, timestamp: this.route.snapshot.queryParams.timestamp });
      } else {
        delete this.log;
      }
    });
  }

  getStatusColor(status) {
    switch (this.getCodeByStatus(status)) {
      case 1: return 'black';
      case 2: return 'green';
      case 3: return '#dbdb0a';
      case 4: return 'orange';
      case 5: return 'red';
    }
  }

  getMethodColor(method) {
    switch (method.toUpperCase()) {
      case 'GET':
        return '#43a047';
      case 'POST':
        return '#fb8c00';
      case 'PUT':
        return '#039be5';
      case 'DELETE':
        return '#e53935';
      default:
        return '#757575';
    }
  }

  private buildPaginationData(total) {
    const totalPages = total / this.size;
    this.paginationData = {
      first: 1,
      last: totalPages,
      current_page: this.route.snapshot.queryParams[SearchQueryParam.PAGE] || 1,
      total_pages: totalPages,
      total
    };
  }

  getOrder() {
    const field = this.route.snapshot.queryParams[SearchQueryParam.FIELD];
    if (field) {
      if ('DESC' === this.route.snapshot.queryParams[SearchQueryParam.ORDER]) {
        return '-' + field;
      }
      return field;
    }
    return '-timestamp';
  }

  getCodeByStatus(status) {
    return Math.round(status / 100);
  }

  getLang(headers) {
    if (headers['Content-Type']) {
      const contentType = headers['Content-Type'][0];
      const contentTypes = contentType.split(';', 1);
      return contentTypes[0].split('/')[1];
    }
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    if (this.paginationData.current_page !== page) {
      const queryParams: any = {};
      queryParams[SearchQueryParam.PAGE] = page;
      queryParams[SearchQueryParam.SIZE] = this.size;
      queryParams.log = null;
      this.router.navigate([], {
        queryParams,
        queryParamsHandling: 'merge',
        fragment: this.analyticsService.fragment
      }).then(() => {
        this.refresh();
      });
    }
  }

  @HostListener(':gv-table:sort', ['$event.detail'])
  _onSort({ order }) {
    const desc = order.startsWith('-');
    const queryParams: any = {};
    queryParams[SearchQueryParam.FIELD] = desc ? order.substring(1) : order;
    queryParams[SearchQueryParam.ORDER] = desc ? 'DESC' : 'ASC';
    queryParams.log = null;
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    }).then(() => {
      this.refresh();
    });
  }

  onSelectSize(size) {
    this.router.navigate([], {
      queryParams: { size, page: null, log: null },
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    }).then(() => {
      this.size = size;
      this.refresh();
    });
  }

  @HostListener(':gv-table:select', ['$event.detail.items[0]'])
  onSelectLog(log: Log) {
    this.router.navigate([], {
      queryParams: { log: log ? log.id : null,  timestamp: log ? log.timestamp : null },
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment
    }).then(() => {
      if (log) {
        this.applicationService.getApplicationLogByApplicationIdAndLogId({
          applicationId: this.route.snapshot.data.application.id,
          logId: log.id,
          timestamp: log.timestamp,
        }).toPromise().then(l => {
          this.log = l;
          if (this.log.request) {
            this.requestHeaders = Object.keys(this.log.request.headers).map((key) => {
              return [key, this.log.request.headers[key]];
            });
          }
          if (this.log.response) {
            this.responseHeaders = Object.keys(this.log.response.headers).map((key) => {
              return [key, this.log.response.headers[key]];
            });
          }
          setTimeout(() => {
            document.getElementById('log').scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'nearest' });
          });
        });
      } else {
        delete this.log;
      }
    });
  }
}
