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

class LogsFiltersController {
  public filters: any = {};
  public methods = {
    1: 'CONNECT',
    2: 'DELETE',
    3: 'GET',
    4: 'HEAD',
    5: 'OPTIONS',
    6: 'PATCH',
    7: 'POST',
    8: 'PUT',
    9: 'TRACE'
  };
  public responseTimes = {
    '[0 TO 100]': '0 to 100ms',
    '[100 TO 200]': '100 to 200ms',
    '[200 TO 300]': '200 to 300ms',
    '[300 TO 400]': '300 to 400ms',
    '[400 TO 500]': '400 to 500ms',
    '[500 TO *]': '> 500ms'
  };
  public httpStatus = {
    100: 'CONTINUE 100',
    101: 'SWITCHING PROTOCOLS 101',
    102: 'PROCESSING 102',
    200: 'OK 200',
    201: 'CREATED 201',
    202: 'ACCEPTED 202',
    203: 'NON AUTHORITATIVE INFORMATION 203',
    204: 'NO CONTENT 204',
    205: 'RESET CONTENT 205',
    206: 'PARTIAL CONTENT 206',
    207: 'MULTI STATUS 207',
    300: 'MULTIPLE CHOICES 300',
    301: 'MOVED PERMANENTLY 301',
    302: 'FOUND 302',
    303: 'SEE OTHER 303',
    304: 'NOT MODIFIED 304',
    305: 'USE PROXY 305',
    307: 'TEMPORARY REDIRECT 307',
    400: 'BAD REQUEST 400',
    401: 'UNAUTHORIZED 401',
    402: 'PAYMENT REQUIRED 402',
    403: 'FORBIDDEN 403',
    404: 'NOT FOUND 404',
    405: 'METHOD NOT ALLOWED 405',
    406: 'NOT ACCEPTABLE 406',
    407: 'PROXY AUTHENTICATION REQUIRED 407',
    408: 'REQUEST TIMEOUT 408',
    409: 'CONFLICT 409',
    410: 'GONE 410',
    411: 'LENGTH REQUIRED 411',
    412: 'PRECONDITION FAILED 412',
    413: 'REQUEST ENTITY TOO LARGE 413',
    414: 'REQUEST URI TOO LONG 414',
    415: 'UNSUPPORTED MEDIA TYPE 415',
    416: 'REQUESTED RANGE NOT SATISFIABLE 416',
    417: 'EXPECTATION FAILED 417',
    422: 'UNPROCESSABLE ENTITY 422',
    423: 'LOCKED 423',
    424: 'FAILED DEPENDENCY 424',
    429: 'TOO MANY REQUESTS 429',
    500: 'INTERNAL SERVER ERROR 500',
    501: 'NOT IMPLEMENTED 501',
    502: 'BAD GATEWAY 502',
    503: 'SERVICE UNAVAILABLE 503',
    504: 'GATEWAY TIMEOUT 504',
    505: 'HTTP VERSION NOT SUPPORTED 505',
    507: 'INSUFFICIENT STORAGE 507'
  };
  private fields = {
    'responseTime': 'response-time',
    'id': '_id'
  };
  private onFiltersChange: any;
  private reordererdMedata: any;
  private $scope;

  constructor($scope) {
    'ngInject';
    this.$scope = $scope;
  }

  $onChanges(changesObj) {
    if (changesObj.metadata) {
      let metadata = changesObj.metadata.currentValue;
      if (metadata) {
        this.reordererdMedata = this.swap(metadata);
      }
    }
  };

  search() {
    let query = this.buildQuery(this.filters);
    this.onFiltersChange({filters : query});
  }

  clearFilters() {
    this.$scope.logsFiltersForm.$setPristine();
    this.filters = {};
    this.search();
  }

  hasFilters() {
    return !_.isEmpty(this.filters) && !this.isEmpty(this.filters);
  }

  private isEmpty(map) {
    for(let key in map) {
      let val = map[key];
      if (val !== undefined && val.length > 0) {
        return false;
      }
    }
    return true;
  }

  private buildQuery(filters): string {
    let query = '';
    let keys = _.filter(Object.keys(filters), key =>  filters[key] !== undefined && filters[key].length > 0);
    let index = 0;
    let that = this;
    _.forEach( keys, key => {
      let val = filters[key];
      if (key === 'application' || key === 'plan') {
        val = that.map(val, that.reordererdMedata, false);
      }
      if (key === 'path') {
        if (!val.startsWith('/')) {
          val = '/' + val;
        }
        val = val.replace(/\//g, '//');
      }
      let params = (val.constructor === Array && val.length > 1) ? that.convert(val) : val;
      query += that.map(key, that.fields, true) + ':' + params;
      if (index + 1 < keys.length) {
        query += ' AND ';
      }
      index++;
    });
    return query;
  }

  private convert(params) {
    return '(' + params.join(' OR ') + ')';
  }

  private map(_val, list, strict) {
    let val = null;
    if (strict) {
      val = list[_val];
    } else {
      val = list[_.filter(Object.keys(list), elt => elt.toLowerCase().includes(_val.toLowerCase())).pop()];
    }
    return (val) ? val : _val;
  }

  private swap(metadata) {
    let ret = {};
    for(let key in metadata){
      ret[metadata[key].name] = key;
    }
    return ret;
  }
}

export default LogsFiltersController;
