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

import { ActivatedRoute, Params } from '@angular/router';
import { clone, forEach, startsWith } from 'lodash';

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

  constructor(
    private $http,
    private Constants,
  ) {}

  /*
   * Analytics
   */
  analytics(request) {
    let url = `${this.Constants.env.baseURL}/platform/analytics` + '?';
    const keys = Object.keys(request);
    forEach(keys, (key) => {
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

  getQueryFilters(activatedRoute: ActivatedRoute) {
    const q = activatedRoute?.snapshot?.queryParams?.q;
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

  buildQueryFromState(queryParams: Params) {
    const query = new LogsQuery();
    query.page = queryParams.page || 1;
    query.size = queryParams.size || 15;
    query.from = queryParams.from;
    query.to = queryParams.to;
    query.query = queryParams.q;
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
    forEach(keys, (key) => {
      const val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });
    return url;
  }

  private cloneQuery(query: LogsQuery) {
    const clonedQuery = clone(query);
    if (startsWith(clonedQuery.field, '-')) {
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
AnalyticsService.$inject = ['$http', 'Constants'];

export default AnalyticsService;
