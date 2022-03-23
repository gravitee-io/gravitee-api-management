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
import { Component, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import '@gravitee/ui-components/wc/gv-chart-line';
import '@gravitee/ui-components/wc/gv-chart-pie';
import '@gravitee/ui-components/wc/gv-chart-map';
import '@gravitee/ui-components/wc/gv-stats';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';

import { GvAnalyticsFiltersComponent } from '../../../components/gv-analytics-filters/gv-analytics-filters.component';
import { ApplicationService, Log } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { AnalyticsService } from '../../../services/analytics.service';
import { ScrollService } from '../../../services/scroll.service';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';
import { ConfigurationService } from '../../../services/configuration.service';

@Component({
  selector: 'app-application-logs',
  templateUrl: './application-logs.component.html',
  styleUrls: ['./application-logs.component.css'],
})
export class ApplicationLogsComponent implements OnInit, OnDestroy {
  private subscription: any;
  logs: Array<Log>;
  selectedLogIds: string[];
  selectedLog;
  options: any;
  format: any;
  paginationData: any = {};
  pageSizes: Array<any>;
  size: number;
  requestHeaders: Array<any>;
  responseHeaders: Array<any>;
  link: { label: string; relativePath: string; icon: string };
  isExporting: boolean;
  isSearching: boolean;

  @ViewChild(GvAnalyticsFiltersComponent)
  filtersComponent: GvAnalyticsFiltersComponent;

  constructor(
    private applicationService: ApplicationService,
    private analyticsService: AnalyticsService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private config: ConfigurationService,
    private scrollService: ScrollService,
  ) {}

  ngOnInit(): void {
    this.pageSizes = this.config.get('pagination.size.values');
    this.size = this.route.snapshot.queryParams[SearchQueryParam.SIZE]
      ? parseInt(this.route.snapshot.queryParams[SearchQueryParam.SIZE], 10)
      : this.config.get('pagination.size.default');
    this.subscription = this.route.queryParams.subscribe(queryParams => {
      if (queryParams && !queryParams.skipRefresh) {
        this.refresh(queryParams);
      }
    });
    this.translateService.get('application.logs.displayAnalytics').subscribe(displayAnalytics => {
      this.link = { label: displayAnalytics, relativePath: '../analytics', icon: 'shopping:chart-line#1' };
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  async refresh(queryParams) {
    const application = this.route.snapshot.data.application;
    if (application) {
      this.isSearching = true;
      const response = await this.applicationService.getApplicationLogs(this.getRequestParameters(queryParams, application)).toPromise();
      this.isSearching = false;
      this.logs = response.data;

      const metadata = response.metadata;
      this.buildPaginationData(response.metadata.data.total);
      this.format = key => this.translateService.get(key).toPromise();
      this.options = {
        selectable: true,
        data: [
          { field: 'timestamp', type: 'datetime', label: i18n('application.logs.date'), style: 'color: #40A9FF', width: '200px' },
          {
            tag: 'status',
            label: i18n('application.logs.status'),
            style: ({ status }) => {
              const color = this.getStatusColor(status);
              return `--gv-tag--bdc: ${color}; --gv-tag--c: ${color};`;
            },
          },
          { field: 'api', label: i18n('application.logs.api'), format: item => metadata[item].name },
          { field: 'plan', label: i18n('application.logs.plan'), format: item => metadata[item].name },
          {
            field: 'method',
            label: i18n('application.logs.method'),
            format: item => item.toUpperCase(),
            style: ({ method }) => 'color:' + this.getMethodColor(method),
          },
          {
            field: 'path',
            label: i18n('application.logs.path'),
            width: '350px',
            style: () => '--gv-table-cell--d: block; height: auto; text-overflow: ellipsis; white-space: nowrap; overflow: hidden;',
          },
          {
            field: 'responseTime',
            label: i18n('application.logs.responseTime'),
            headerStyle: () => 'justify-content: flex-end',
            format: item => item + ' ms',
            style: () => 'text-align: right',
          },
        ],
      };
      if (queryParams.log && queryParams.timestamp) {
        this.selectedLogIds = this.logs.filter(l => l.id === queryParams.log).map(log => log.id);
        this._loadLog({ id: queryParams.log, timestamp: queryParams.timestamp });
      } else {
        this.selectedLogIds = [];
      }
    }
  }

  private getRequestParameters(queryParams, application) {
    const timeSlot = this.analyticsService.getTimeSlotFromQueryParams();
    let field = queryParams[SearchQueryParam.FIELD] || '@timestamp';
    field = field === 'timestamp' ? '@timestamp' : field;
    field = field === 'responseTime' ? 'response-time' : field;
    return {
      applicationId: application.id,
      from: timeSlot.from,
      to: timeSlot.to,
      size: this.size,
      page: queryParams[SearchQueryParam.PAGE] || 1,
      field,
      order: queryParams[SearchQueryParam.ORDER] || 'DESC',
      query: this.analyticsService.getQueryFromPath().query,
    };
  }

  getStatusColor(status) {
    switch (this.getCodeByStatus(status)) {
      case 1:
        return 'black';
      case 2:
        return 'green';
      case 3:
        return '#dbdb0a';
      case 4:
        return 'orange';
      case 5:
        return 'red';
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
      total,
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
    const contentTypeHeaderKey = Object.keys(headers).find(header => 'content-type' === header.toLowerCase());
    const contentTypeHeader = headers[contentTypeHeaderKey];
    if (contentTypeHeader) {
      const contentType = contentTypeHeader[0];
      const contentTypes = contentType.split(';', 1);
      return contentTypes[0].split('/')[1];
    }
  }

  @HostListener(':gv-pagination:paginate', ['$event.detail'])
  _onPaginate({ page }) {
    const queryParams: any = {};
    queryParams[SearchQueryParam.PAGE] = page;
    queryParams[SearchQueryParam.SIZE] = this.size;
    queryParams.log = null;
    this.router.navigate([], {
      queryParams,
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment,
    });
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
      fragment: this.analyticsService.fragment,
    });
  }

  onSelectSize(size) {
    this.size = size;
    this.router.navigate([], {
      queryParams: { size, page: null, log: null },
      queryParamsHandling: 'merge',
      fragment: this.analyticsService.fragment,
    });
  }

  @HostListener(':gv-table:select', ['$event.detail.items[0]'])
  onSelectLog(log: Log) {
    if (log) {
      this.router.navigate([], {
        queryParams: { log: log ? log.id : null, timestamp: log ? log.timestamp : null },
        queryParamsHandling: 'merge',
        fragment: this.analyticsService.fragment,
      });
    } else {
      this.selectedLog = null;
      this.selectedLogIds = [];
    }
  }

  async _loadLog({ id, timestamp }) {
    this.selectedLog = await this.applicationService
      .getApplicationLogByApplicationIdAndLogId({
        applicationId: this.route.snapshot.data.application.id,
        logId: id,
        timestamp,
      })
      .toPromise();

    if (this.selectedLog.request) {
      this.requestHeaders = Object.keys(this.selectedLog.request.headers).map(key => [key, this.selectedLog.request.headers[key]]);
    }
    if (this.selectedLog.response) {
      this.responseHeaders = Object.keys(this.selectedLog.response.headers).map(key => [key, this.selectedLog.response.headers[key]]);
    }
    this.scrollService.scrollToAnchor('log');
  }

  export() {
    const queryParams = this.route.snapshot.queryParams;
    const application = this.route.snapshot.data.application;
    this.isExporting = true;
    const logsQuery = this.getRequestParameters(queryParams, application);
    logsQuery.page = 1;
    logsQuery.size = 10000;
    this.applicationService
      .exportApplicationLogsByApplicationId(logsQuery)
      .toPromise()
      .then(response => {
        const hiddenElement = document.createElement('a');
        hiddenElement.href = 'data:attachment/csv,' + encodeURIComponent(response);
        hiddenElement.target = '_self';
        let fileName = 'logs-' + application.name + '-' + Date.now();
        fileName = fileName.replace(/[\s]/gi, '-');
        fileName = fileName.replace(/[^\w]/gi, '-');
        hiddenElement.download = fileName + '.csv';
        document.getElementById('hidden-export-container').appendChild(hiddenElement);
        hiddenElement.click();
        document.getElementById('hidden-export-container').removeChild(hiddenElement);
      })
      .finally(() => (this.isExporting = false));
  }
}
