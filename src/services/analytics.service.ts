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

class AnalyticsService {
  private analyticsURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.analyticsURL = `${Constants.baseURL}platform/analytics`;
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

    return this.$http.get(url);
  }

  hitsBy(key, query, field, aggType, from, to, interval) {
    return this.$http.get(this.analyticsURL + '?type=hits_by&key=' + key + '&query=' + query + '&field=' + field + '&aggType=' + aggType + '&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  globalHits(from, to, interval, key, query) {
    return this.$http.get(this.analyticsURL + '?type=global_hits&key=' + key + '&query=' + query + '&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  topHits(from, to, interval, key, query, field, orderField, orderDirection, orderMode, size) {
    return this.$http.get(this.analyticsURL + '?type=top_hits&key=' + key +
                                              '&query=' + query +
                                              '&field=' + field +
                                              ((orderField) ? '&orderField=' + orderField : "") +
                                              ((orderDirection) ?'&orderDirection=' + orderDirection : "") +
                                              ((orderMode) ? '&orderMode=' + orderMode : "") +
                                              '&interval=' + interval +
                                              '&from=' + from +
                                              '&to=' + to +
                                              '&size=' + size);
  }

}

export default AnalyticsService;
