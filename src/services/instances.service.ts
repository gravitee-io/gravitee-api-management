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
class InstancesService {
  private instancesURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.instancesURL = `${Constants.envBaseURL}/instances/`;
  }

  search(includeStopped?: boolean, from?: number, to?: number, page?: number, size?: number) {
    if (includeStopped === undefined) {
      includeStopped = false;
    }

    if (from === undefined) {
      from = 0;
    }

    if (to === undefined) {
      to = 0;
    }

    if (page === undefined) {
      page = 0;
    }

    if (size === undefined) {
      size = 100;
    }

    return this.$http.get(`${this.instancesURL}?includeStopped=${includeStopped}&from=${from}&to=${to}&page=${page}&size=${size}`);
  }

  get(id) {
    return this.$http.get(this.instancesURL + id);
  }

  getMonitoringData(id: string, gatewayId: string): any {
    return this.$http.get(this.instancesURL + id + '/monitoring/' + gatewayId);
  }
}

export default InstancesService;
