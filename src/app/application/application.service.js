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
  constructor($http, baseURL) {
		'ngInject';
    this.$http = $http;
		this.baseURL = baseURL;
    this.applicationsURL = baseURL + 'applications/';
  }

	get(name) {
    return this.$http.get(this.applicationsURL + name);
  }
	
	list() {
    return this.$http.get(this.baseURL + 'user/applications/');
  }

	create(application) {
    return this.$http.post(this.baseURL + 'user/applications/', application);
  }

  update(application) {
    return this.$http.put(this.applicationsURL + application.name,
      {'description': application.description, 'type': application.type}
    );
  }

  delete(name) {
    return this.$http.delete(this.applicationsURL + name);
  }
}

export default ApplicationService;
