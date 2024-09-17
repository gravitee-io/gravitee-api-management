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
import * as _ from 'lodash';

export class LogsQuery {
  from: number;
  to: number;
  query?: string;
  page: number;
  size: number;
  field: string;
  order: boolean;
}

class AnalyticsService {
  private platformUrl: string;
  private analyticsURL: string;
  private logs: [any];

  /* @ngInject */
  constructor(private $http, private Constants, public $stateParams) {}

  /*
   * Analytics
   */
  analytics(request) {
    let url = `${this.Constants.env.baseURL}/platform/analytics` + '?';
    const keys = Object.keys(request);
    _.forEach(keys, (key) => {
      const val = request[key];
      if (val !== null && val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });

    return this.$http.get(url, { timeout: this.getAnalyticsHttpTimeout() });
  }

  getAnalyticsHttpTimeout() {
    return this.Constants.env.settings.analytics.clientTimeout as number;
  }

  findLogs(query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), `${this.Constants.env.baseURL}/platform` + '/logs?'), {
      timeout: 30000,
    });
  }

  exportLogsAsCSV(query: LogsQuery): ng.IPromise<any> {
    const logsQuery = this.cloneQuery(query);
    logsQuery.page = 1;
    logsQuery.size = 10000;
    return this.$http.get(this.buildURLWithQuery(logsQuery, `${this.Constants.env.baseURL}/platform` + '/logs/export?'), {
      timeout: 30000,
    });
  }

  getLog(logId, timestamp) {
    return this.$http.get(`${this.Constants.env.baseURL}/platform` + '/logs/' + logId + (timestamp ? '?timestamp=' + timestamp : ''));
  }

  getQueryFilters() {
    const q = this.$stateParams.q;
    if (q) {
      const queryFilters = {};
      q.split(/\s(OR|AND)\s/).forEach((q) => {
        if (q.includes(':')) {
          const keyParam = this.cleanParam(q.substring(0, q.indexOf(':')));
          const valueParam = this.cleanParam(q.substring(q.indexOf(':') + 1));
          if (queryFilters[keyParam]) {
            queryFilters[keyParam].push(valueParam);
          } else {
            queryFilters[keyParam] = [valueParam];
          }
        }
      });
      return queryFilters;
    }
    return null;
  }

  buildQueryParam(queryParam, q: string) {
    queryParam = q === 'body' ? '*' + queryParam + '*' : queryParam;
    queryParam = q === 'uri' ? queryParam + '*' : queryParam;
    if (queryParam !== '?') {
      queryParam = '\\"' + queryParam + '\\"';
      queryParam = queryParam.replace(/\//g, '\\\\/');
    }
    return queryParam;
  }

  buildQueryFromState($state: StateService) {
    const query = new LogsQuery();
    query.page = $state.params.page || 1;
    query.size = $state.params.size || 15;
    query.from = $state.params.from;
    query.to = $state.params.to;
    query.query = $state.params.q;
    query.field = '-@timestamp';
    return query;
  }

  setFetchedLogs(logs) {
    this.logs = logs.map((log) => log.id);
  }

  getFetchedLogs() {
    return this.logs;
  }

  /*
   * Logs
   */
  private buildURLWithQuery(query: LogsQuery, url) {
    const keys = Object.keys(query);
    _.forEach(keys, (key) => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });
    return url;
  }

  private cloneQuery(query: LogsQuery) {
    const clonedQuery = _.clone(query);
    if (_.startsWith(clonedQuery.field, '-')) {
      clonedQuery.order = false;
      clonedQuery.field = clonedQuery.field.substring(1);
    } else {
      clonedQuery.order = true;
    }
    return clonedQuery;
  }

  private cleanParam(param) {
    return param.replace('%20', ' ').replace(/[()]/g, '').replace(/[\\"]/g, '');
  }
}

export default AnalyticsService;
