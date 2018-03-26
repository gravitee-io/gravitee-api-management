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
import _ = require('lodash');
import moment = require("moment");

import ApplicationService, { LogsQuery } from "../../../../services/applications.service";

class ApplicationLogsController {

  private logs: {total: string; logs: any[]};
  private query: LogsQuery;
  private application: any;

  constructor(
    private ApplicationService: ApplicationService,
    private $state: ng.ui.IStateService
  ) {
  'ngInject';
    this.ApplicationService = ApplicationService;

    this.onPaginate = this.onPaginate.bind(this);

    this.query = new LogsQuery();
    this.query.size = 20;
    this.query.page = 1;
  }

  $onInit() {
      this.query.from = this.$state.params['from'];
      this.query.to = this.$state.params['to'];
      this.query.query = this.$state.params['q'];
  };

  timeframeChange(timeframe) {
    this.query.from = timeframe.from;
    this.query.to = timeframe.to;
    this.query.page = 1;
    this.refresh();
  }

  onPaginate(page) {
    this.query.page = page;
    this.refresh();
  }

  refresh() {
    this.ApplicationService.findLogs(this.application.id, this.query).then((logs) => {
      this.logs = logs.data;
    });
  }
}

export default ApplicationLogsController;
