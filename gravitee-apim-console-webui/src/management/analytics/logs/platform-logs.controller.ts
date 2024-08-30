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
import { IScope } from 'angular';

import { ActivatedRoute, Router } from '@angular/router';
import { now } from 'lodash';

import { ApiService } from '../../../services/api.service';
import ApplicationService from '../../../services/application.service';
import AnalyticsService, { LogsQuery } from '../../../services/analytics.service';

class PlatformLogsController {
  private logs: { total: string; logs: any[]; metadata: any };
  private query: LogsQuery;
  private metadata: {
    apis?: any[];
    applications?: any[];
  };
  private init: boolean;
  private activatedRoute: ActivatedRoute;
  private isLoaded = false;

  constructor(
    private ApiService: ApiService,
    private AnalyticsService: AnalyticsService,
    private Constants,
    private ApplicationService: ApplicationService,
    private ngRouter: Router,
    private $scope: IScope,
  ) {
    this.query = new LogsQuery();
  }

  $onInit() {
    this.onPaginate = this.onPaginate.bind(this);
    this.query.page = this.activatedRoute.snapshot.queryParams.page || 1;
    this.query.size = this.activatedRoute.snapshot.queryParams.size || 15;
    this.query.from = this.activatedRoute.snapshot.queryParams.from;
    this.query.to = this.activatedRoute.snapshot.queryParams.to;
    this.query.query = this.activatedRoute.snapshot.queryParams.q;
    this.query.field = '-@timestamp';

    this.$scope.$watch('$ctrl.query.field', (field) => {
      if (field && this.init) {
        this.refresh();
      }
    });

    Promise.all([this.ApiService.list(null, false, 1, null, null, null, 10), this.ApplicationService.list(['owner', 'picture'])]).then(
      ([apis, applications]) => {
        this.metadata = {
          apis: apis.data.data,
          applications: applications.data,
        };
        this.isLoaded = true;
      },
    );
  }

  timeframeChange(timeframe) {
    this.init = true;
    this.query.from = timeframe.from;
    this.query.to = timeframe.to;
    this.query.page = this.activatedRoute.snapshot.queryParams.page || 1;
    this.refresh();
  }

  onPaginate(page) {
    this.query.page = page;
    this.refresh();
  }

  refresh() {
    this.ngRouter.navigate(['.'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        page: this.query.page,
        size: this.query.size,
        from: this.query.from,
        to: this.query.to,
        q: this.query.query,
      },
      queryParamsHandling: 'merge',
    });

    this.AnalyticsService.findLogs(this.query).then((logs) => {
      this.logs = logs.data;
    });
  }

  filtersChange(filters) {
    this.query.page = this.activatedRoute.snapshot.queryParams.page || 1;
    this.query.query = filters;
    this.refresh();
  }

  exportAsCSV() {
    this.AnalyticsService.exportLogsAsCSV(this.query).then((response) => {
      const hiddenElement = document.createElement('a');
      hiddenElement.href = 'data:attachment/csv,' + response.data;
      hiddenElement.target = '_self';
      let fileName = 'logs-platform-' + now();
      fileName = fileName.replace(/[\s]/gi, '-');
      fileName = fileName.replace(/[^\w]/gi, '-');
      hiddenElement.download = fileName + '.csv';
      hiddenElement.click();
      document.body.removeChild(hiddenElement);
    });
  }

  showLogDetails(log) {
    const fullUrl = window.location.href;
    const index = fullUrl.indexOf('/#!');
    const baseUrl = fullUrl.substring(0, index + 3);

    return (
      baseUrl +
      this.ngRouter.createUrlTree(['.', log.id], {
        relativeTo: this.activatedRoute,
        queryParams: {
          timestamp: log.timestamp,
          from: this.query.from,
          to: this.query.to,
          q: this.query.query,
          page: this.query.page,
          size: this.query.size,
        },
      })
    );
  }
}

PlatformLogsController.$inject = ['ApiService', 'AnalyticsService', 'Constants', 'ApplicationService', 'ngRouter', '$scope'];

export default PlatformLogsController;
