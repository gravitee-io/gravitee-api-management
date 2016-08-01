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

	create(application) {
    return this.$http.post(this.applicationsURL, application);
  }

  update(application) {
    return this.$http.put(this.applicationsURL + application.id,
      {'name': application.name, 'description': application.description, 'type': application.type}
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
}

export default ApplicationService;
