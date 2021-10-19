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
import ApplicationCreationController from './application-creation.controller';

import { ApplicationType } from '../../../../entities/application';

class ApplicationCreationStep2Controller {
  private selectedType: ApplicationType;
  private parent: ApplicationCreationController;

  constructor(private Constants) {
    'ngInject';
  }

  $onInit() {
    this.selectedType = this.parent.enabledApplicationTypes[0];
  }

  selectType(applicationType: ApplicationType) {
    this.selectedType = applicationType;
    if (this.selectedType.isOauth()) {
      this.parent.application.settings = {
        oauth: {
          application_type: this.selectedType.id,
          grant_types: this.selectedType.default_grant_types.map((grant) => grant.type),
          redirect_uris: [],
        },
      };
    } else {
      this.parent.application.settings = {
        app: {},
      };
    }
  }

  displaySimpleAppConfig() {
    return !this.selectedType.isOauth();
  }

  displayRedirectUris() {
    return this.selectedType.isOauth() && this.selectedType.requires_redirect_uris;
  }
}

export default ApplicationCreationStep2Controller;
