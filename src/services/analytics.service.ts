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
  private analyticsHttpTimeout: number;

  constructor(private $http, Constants, public $stateParams) {
    'ngInject';
    this.platformUrl = `${Constants.envBaseURL}/platform`;
    this.analyticsURL = `${Constants.envBaseURL}/platform/analytics`;
    this.analyticsHttpTimeout = Constants.analytics.clientTimeout as number;
  }

  /*
   * Analytics
   */
  analytics(request) {
    var url = this.analyticsURL + '?';
    var keys = Object.keys(request);
    _.forEach(keys, function (key) {
      var val = request[key];
      if (val !== undefined) {
        url += key + '=' + val + '&';
      }
    });


    return this.$http.get(url, { timeout: this.analyticsHttpTimeout });
  }

  findLogs(query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), this.platformUrl + '/logs?'), { timeout: 30000 });
  }

  exportLogsAsCSV(query: LogsQuery): ng.IPromise<any> {
    return this.$http.get(this.buildURLWithQuery(this.cloneQuery(query), this.platformUrl + '/logs/export?'), { timeout: 30000 });
  }

  getLog(logId, timestamp) {
    return this.$http.get(this.platformUrl + '/logs/' + logId + ((timestamp) ? '?timestamp=' + timestamp : ''));
  }

  getQueryFilters() {
    let q = this.$stateParams.q;
    if (q) {
      const queryFilters = {};
      q.split(/\s(OR|AND)\s/).forEach(q => {
        if (q.includes(':')) {
          let keyParam = this.cleanParam(q.substring(0, q.indexOf(':')));
          let valueParam = this.cleanParam(q.substring(q.indexOf(':') + 1));
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
    queryParam = (q === 'body') ? ('*' + queryParam + '*') : queryParam;
    queryParam = (q === 'uri') ? (queryParam + '*') : queryParam;
    if (queryParam !== '?') {
      queryParam = '\\"' + queryParam + '\\"';
      queryParam = queryParam.replace(/\//g, '\\\\/');
    }
    return queryParam;
  }

  /*
   * Logs
   */
  private buildURLWithQuery(query: LogsQuery, url) {
    var keys = Object.keys(query);
    _.forEach(keys, function (key) {
      var val = query[key];
      if (val !== undefined && val !== '') {
        url += key + '=' + val + '&';
      }
    });
    return url;
  }

  private cloneQuery(query: LogsQuery) {
    let clonedQuery = _.clone(query);
    if (_.startsWith(clonedQuery.field, '-')) {
      clonedQuery.order = false;
      clonedQuery.field = clonedQuery.field.substring(1);
    } else {
      clonedQuery.order = true;
    }
    return clonedQuery;
  }

  private cleanParam(param) {
    return param.replace('%20', ' ').replace(/[()]/g, '').replace(/[\\\"]/g, '');
  }
}

export default AnalyticsService;
