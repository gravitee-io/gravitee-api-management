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
import {ApplicationType} from "../../../../entities/application";
import {GrantType} from "../../../../entities/oauth";
import _ = require('lodash');

const ApplicationCreationStep2Component: ng.IComponentOptions = {
  require: {
    parent: '^createApplication'
  },
  template: require("./application-creation-step2.html"),
  controller: function(Constants) {
    'ngInject';
    this.allowedTypes = _.filter([...ApplicationType.TYPES], (type: ApplicationType) => {
      return Constants.application.types[type.value.toLowerCase()].enabled;
    });
    this.grantTypes = GrantType.TYPES;
    this.selectedType = this.allowedTypes[0];

    this.selectType = function(applicationType: ApplicationType) {
      this.selectedType = applicationType;

      if (this.selectedType.oauth) {
        this.parent.application.settings = {
          oauth: _.merge({
            grant_types: this.selectedType.oauth.default_grant_types
          }, this.selectedType.configuration.oauth)
        };

        // Update response_types according to the selected grant type
        this.updateGrantTypes();
      } else {
        this.parent.application.settings = {
          app: {}
        }
      }
    };

    this.updateGrantTypes = () => {
      this.parent.application.settings.oauth.response_types =
        _.flatMap(this.parent.application.settings.oauth.grant_types,
          (selected) => _.find(this.grantTypes, (grantType) => grantType.type === selected).response_types);
    };

    this.displaySimpleAppConfig = () => {
      if (!this.allowedTypes || this.allowedTypes.length === 0) {
        return true;
      }
      return !this.parent.clientRegistrationEnabled() || this.selectedType.value === 'SIMPLE';
    }
  }
};

export default ApplicationCreationStep2Component;
