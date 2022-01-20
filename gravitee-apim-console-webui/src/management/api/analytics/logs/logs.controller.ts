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
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';
import * as _ from 'lodash';

import AnalyticsService from '../../../../services/analytics.service';
import { ApiService, LogsQuery } from '../../../../services/api.service';

class ApiLogsController {
  private api: any;
  private logs: { total: string; logs: any[]; metadata: any };
  private query: LogsQuery;
  private metadata: {
    applications: any[];
    plans: any[];
    tenants?: any[];
  };
  private init: boolean;

  constructor(
    private ApiService: ApiService,
    private resolvedApi,
    private plans: any,
    private applications: any,
    private tenants: any,
    private $scope: IScope,
    private Constants,
    private $state: StateService,
    private $timeout: ng.ITimeoutService,
    private AnalyticsService: AnalyticsService,
  ) {
    'ngInject';
    this.ApiService = ApiService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;
    this.metadata = {
      applications: applications.data,
      plans: plans.data,
    };

    const hasTenants = _.chain(this.api.proxy.groups)
      .map((group) => group.endpoints)
      .find((endpoint) => _.has(endpoint, 'tenants'));

    if (hasTenants !== undefined) {
      this.metadata.tenants = tenants.data;
    }

    this.onPaginate = this.onPaginate.bind(this);

    this.query = this.AnalyticsService.buildQueryFromState(this.$state);

    this.$scope.$watch('logsCtrl.query.field', (field) => {
      if (field && this.init) {
        this.refresh();
      }
    });
  }

  timeframeChange(timeframe) {
    this.init = true;
    this.query.from = timeframe.from;
    this.query.to = timeframe.to;
    this.query.page = this.$state.params.page || 1;
    this.refresh();
  }

  onPaginate(page) {
    this.query.page = page;
    this.refresh();
  }

  refresh() {
    this.$state.transitionTo(
      this.$state.current,
      {
        apiId: this.api.id,
        page: this.query.page,
        size: this.query.size,
        from: this.query.from,
        to: this.query.to,
        q: this.query.query,
      },
      { notify: false },
    );
    this.ApiService.findLogs(this.api.id, this.query).then((logs) => {
      this.logs = logs.data;
      this.AnalyticsService.setFetchedLogs(logs.data.logs);
    });
  }

  getMetadata(id) {
    return this.logs.metadata[id];
  }

  filtersChange(filters) {
    this.query.page = this.$state.params.page || 1;
    this.query.query = filters;
    this.refresh();
  }

  exportAsCSV() {
    this.ApiService.exportLogsAsCSV(this.api.id, this.query).then((response) => {
      const hiddenElement = document.createElement('a');
      hiddenElement.href = 'data:attachment/csv,' + encodeURIComponent(response.data);
      hiddenElement.target = '_self';
      let fileName = 'logs-' + this.api.name + '-' + this.api.version + '-' + _.now();
      fileName = fileName.replace(/[\s]/gi, '-');
      fileName = fileName.replace(/[^\w]/gi, '-');
      hiddenElement.download = fileName + '.csv';
      document.getElementById('hidden-export-container').appendChild(hiddenElement);
      this.$timeout(() => {
        hiddenElement.click();
      });
      document.getElementById('hidden-export-container').removeChild(hiddenElement);
    });
  }
}

export default ApiLogsController;
