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

import { ApplicationType } from '../entities/application';

class ApplicationTypesService {
  private URL: string;

  constructor(private $http: ng.IHttpService, Constants) {
    'ngInject';
    this.URL = `${Constants.envBaseURL}/configuration/applications/types`;
  }

  getEnabledApplicationTypes(): ng.IHttpPromise<Array<ApplicationType>> {
    return this.$http.get(this.URL);
  }

}

export default ApplicationTypesService;
