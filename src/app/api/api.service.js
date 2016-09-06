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

  list(view) {
    return this.$http.get(this.apisURL + (view?'?view=' + view : ''));
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
        'picture': api.picture, 'resources': api.resources, 'views': api.views
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
    return this.$http.post(this.apisURL + (apiId?apiId + '/':'') + 'import', apiDefinition);
  }

  importSwagger(swaggerDescriptor) {
    return this.$http.post(this.apisURL + 'import/swagger', swaggerDescriptor);
  }

  export(apiId) {
    return this.$http.get(this.apisURL + apiId + '/export');
  }

  /*
   * Analytics
   */
  apiHits(api, from, to, interval) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsByStatus(api, from, to, interval) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by_status&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsByLatency(api, from, to, interval) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by_latency&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsByPayloadSize(api, from, to, interval) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by_payload_size&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsByApplication(api, from, to, interval) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by_application&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiHitsBy(api, key, query, field, aggType, from, to, interval) {
    return this.$http.get(this.apisURL + api + '/analytics?type=hits_by&key=' + key + '&query=' + query + '&field=' + field + '&aggType=' + aggType + '&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  apiGlobalHits(api, from, to, interval, key, query) {
    return this.$http.get(this.apisURL + api + '/analytics?type=global_hits&key=' + key + '&query=' + query + '&interval=' + interval + '&from=' + from + '&to=' + to)
  }

  apiTopHits(api, from, to, interval, key, query, field, size) {
    return this.$http.get(this.apisURL + api + '/analytics?type=top_hits&key=' + key + '&query=' + query + '&field=' + field + '&interval=' + interval + '&from=' + from + '&to=' + to + '&size=' + size)
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
    return this.$http.post(this.apisURL + api + '/members?user=' + member.username + '&type=' + member.type);
  }

  deleteMember(api, memberUsername) {
    return this.$http.delete(this.apisURL + api + '/members?user=' + memberUsername);
  }

  transferOwnership(api, memberUsername) {
    return this.$http.post(this.apisURL + api + '/members/transfer_ownership?user=' + memberUsername);
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
