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
class ApiService {

  constructor($http, Constants) {
    'ngInject';
    this.$http = $http;
    this.apisURL = Constants.baseURL + 'apis/';
  }

  get(name) {
    return this.$http.get(this.apisURL + name);
  }

  list() {
    return this.$http.get(this.apisURL);
  }

  start(name) {
    return this.$http.post(this.apisURL + name + '?action=START');
  }

  stop(name) {
    return this.$http.post(this.apisURL + name + '?action=STOP');
  }

  reload(name) {
    return this.$http.post(this.apisURL + 'reload/' + name);
  }

  create(api) {
    return this.$http.post(this.apisURL, api);
  }

  update(api) {
    return this.$http.put(this.apisURL + api.id,
      {'version': api.version, 'description': api.description, 'proxy': api.proxy, 'paths': api.paths, 'private': api.private,
        'visibility': api.visibility, 'name': api.name, 'services': api.services, 'properties': api.properties, 'tags': api.tags,
        'picture': api.picture
      }
    );
  }

  delete(name) {
    return this.$http.delete(this.apisURL + name);
  }

  listPolicies(apiName) {
    return this.$http.get(this.apisURL + apiName + '/policies');
  }

  isAPISynchronized(apiId) {
    return this.$http.get(this.apisURL + apiId + '/state');
  }

  deploy(apiId) {
    return this.$http.post(this.apisURL + apiId + '/deploy');
  }

  rollback(apiId, apiDescriptor) {
    return this.$http.post(this.apisURL + apiId + '/rollback', apiDescriptor);
  }

  import(apiId, apiDefinition) {
    return this.$http.post(this.apisURL + (apiId?apiId:'') + '/import', apiDefinition);
  }

  export(apiId) {
    return this.$http.get(this.apisURL + apiId + '/export');
  }

  /*
   * Analytics
   */
  apiHits(api, interval, from, to) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsByStatus(api, interval, from, to) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by_status&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsByLatency(api, interval, from, to) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by_latency&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsByPayloadSize(api, interval, from, to) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by_payload_size&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  /*
   * Health
   */
  apiHealth(api, interval, from, to) {
    return this.$http.get(this.apisURL + api + '/health?interval=' + interval + '&from=' + from + '&to=' + to);
  }

  /*
   * Members
   */
  getMembers(api) {
    return this.$http.get(this.apisURL + api + '/members');
  }

  addOrUpdateMember(api, member) {
    return this.$http.post(this.apisURL + api + '/members?user=' + member.user + '&type=' + member.type);
  }

  deleteMember(api, memberUsername) {
    return this.$http.delete(this.apisURL + api + '/members?user=' + memberUsername);
  }

  /*
   * API Keys
   */
  getApiKeys(api) {
    return this.$http.get(this.apisURL + api + '/keys');
  }

  revokeApiKey(api, apiKey) {
    return this.$http.delete(this.apisURL + api + '/keys/' + apiKey);
  }

  updateApiKey(api, apiKey) {
    return this.$http.put(this.apisURL + api + '/keys/' + apiKey.key, apiKey);
  }

  /*
   * API events
   */
  getApiEvents(api, eventTypes) {
    return this.$http.get(this.apisURL + api + '/events?type=' + eventTypes);
  }
}

export default ApiService;
