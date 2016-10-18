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
class ApplicationService {
  constructor($http, Constants) {
		'ngInject';
    this.$http = $http;
		this.baseURL = Constants.baseURL;
    this.applicationsURL = this.baseURL + 'applications/';
  }

	get(applicationId) {
    return this.$http.get(this.applicationsURL + applicationId);
  }

	getAPIKeys(applicationId) {
		return this.$http.get(this.applicationsURL + applicationId + '/keys');
	}

	getMembers(applicationId) {
		return this.$http.get(this.applicationsURL + applicationId + '/members');
	}

	addOrUpdateMember(applicationId, member) {
		return this.$http.post(this.applicationsURL + applicationId + '/members?user=' + member.username + '&type=' + member.type);
	}

	deleteMember(applicationId, memberUsername) {
		return this.$http.delete(this.applicationsURL + applicationId + '/members?user=' + memberUsername);
	}

  list() {
    return this.$http.get(this.applicationsURL);
  }

  listByGroup(group) {
    return this.$http.get(this.applicationsURL + "?group=" + group);
  }

	create(application) {
    return this.$http.post(this.applicationsURL, application);
  }

  update(application) {
    return this.$http.put(
      this.applicationsURL + application.id,
      {
        'name': application.name,
        'description': application.description,
        'type': application.type,
        'group': application.group ? application.group.id : ''
      }
    );
  }

	subscribe(application, apiId) {
		return this.$http.post(this.applicationsURL + application.id + '/keys?api=' + apiId);
	}

	unsubscribe(application, apiKey) {
		return this.$http.delete(this.applicationsURL + application.id + '/keys/' + apiKey);
	}

  revokeApiKey(application, apiKey) {
    return this.$http.delete(this.applicationsURL + application + '/keys/' + apiKey);
  }

  delete(application) {
    return this.$http.delete(this.applicationsURL + application.id);
  }

  /*
   * Analytics
   */
  applicationHits(application, from, to, interval) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=hits&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  applicationHitsByStatus(application, from, to, interval) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=hits_by_status&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  applicationHitsByLatency(application, from, to, interval) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=hits_by_latency&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  applicationHitsByPayloadSize(application, from, to, interval) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=hits_by_payload_size&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  applicationHitsByApplication(application, from, to, interval) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=hits_by_application&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  applicationHitsBy(application, key, query, field, aggType, from, to, interval) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=hits_by&key=' + key + '&query=' + query + '&field=' + field + '&aggType=' + aggType + '&interval=' + interval + '&from=' + from + '&to=' + to);
  }

  applicationGlobalHits(application, from, to, interval, key, query) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=global_hits&key=' + key + '&query=' + query + '&interval=' + interval + '&from=' + from + '&to=' + to)
  }

  applicationTopHits(application, from, to, interval, key, query, field, size) {
    return this.$http.get(this.applicationsURL + application + '/analytics?type=top_hits&key=' + key + '&query=' + query + '&field=' + field + '&interval=' + interval + '&from=' + from + '&to=' + to + '&size=' + size)
  }

}

export default ApplicationService;
