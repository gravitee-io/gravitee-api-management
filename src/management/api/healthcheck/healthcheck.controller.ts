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
import ApiService, { LogsQuery } from "../../../services/api.service";

class ApiHealthCheckController {
  private api: any;
  private gateway: any;
  private endpoint: any;
  private logs: {total: string; logs: any[], metadata: any};
  private query: LogsQuery;

  constructor (
    private ApiService: ApiService,
    private $scope,
    private $state: ng.ui.IStateService
  ) {
    'ngInject';
    this.api = this.$scope.$parent.apiCtrl.api;
    this.gateway = {availabilities:{},responsetimes:{}};
    this.endpoint = {availabilities:{},responsetimes:{}};

    this.onPaginate = this.onPaginate.bind(this);

    this.query = new LogsQuery();
    this.query.size = 10;
    this.query.page = 1;
    this.query.query = this.$state.params['q'];

    this.updateChart();
  }

  updateChart() {
    this.ApiService.apiHealth(this.api.id, 'availability')
      .then(response => {this.endpoint.availabilities.data = response.data;});

    this.ApiService.apiHealth(this.api.id, 'response_time')
      .then(response => {this.endpoint.responsetimes.data = response.data;});

    this.ApiService.apiHealth(this.api.id, 'availability', 'gateway')
      .then(response => {this.gateway.availabilities.data = response.data;});

    this.ApiService.apiHealth(this.api.id, 'response_time', 'gateway')
      .then(response => {this.gateway.responsetimes.data = response.data;});

    this.refresh();
  }

  onPaginate(page) {
    this.query.page = page;
    this.refresh();
  }

  refresh() {
    this.ApiService.apiHealthLogs(this.api.id, this.query).then((logs) => {
      this.logs = logs.data;
    });
  }

  getMetadata(id) {
    return this.logs.metadata[id];
  }

  getEndpointStatus(state) {
    switch(state) {
      case 3: return 'UP';
      case 2: return 'TRANSITIONALLY_UP';
      case 1: return 'TRANSITIONALLY_DOWN';
      case 0: return 'DOWN';
      default: return '-';
    }
  }

  viewLog(log) {
    this.$state.go('management.apis.detail.healthcheck.log', log);
  }
}

export default ApiHealthCheckController;
