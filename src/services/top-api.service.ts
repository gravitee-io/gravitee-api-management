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

class TopApiService {
  private topApisURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.topApisURL = `${Constants.envBaseURL}/configuration/top-apis/`;
  }

  list() {
    return this.$http.get(this.topApisURL);
  }

  create(topApi) {
    if (topApi) {
      return this.$http.post(this.topApisURL, { api: topApi.id });
    }
  }

  update(topApis) {
    if (topApis && topApis.length) {
      return this.$http.put(this.topApisURL, _.map(topApis, function (topApi: any) {
        return { api: topApi.api };
      }));
    }
  }

  delete(topApi) {
    if (topApi) {
      return this.$http.delete(this.topApisURL + topApi.api);
    }
  }
}

export default TopApiService;
