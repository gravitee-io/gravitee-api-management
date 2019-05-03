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
import ApplicationService from "../../services/application.service";
import NotificationService from "../../services/notification.service";
import {ApplicationType} from "../../entities/application";
import {GrantType} from "../../entities/oauth";
import {StateService} from '@uirouter/core';
import _ = require('lodash');

interface IApplicationScope extends ng.IScope {
  formApplication: any;
}

const CreateApplicationComponent: ng.IComponentOptions = {
  template: require('./create-application.html'),
  controller: class {

    private application: any;
    private selectedType: ApplicationType;

    private types: ApplicationType[];
    private grantTypes = GrantType.TYPES;

    constructor(
      private ApplicationService: ApplicationService,
      private NotificationService: NotificationService,
      private $scope: IApplicationScope,
      private $state: StateService,
      private Constants
    ) {
      'ngInject';

      this.application = {};
    }

    $onInit() {
      if (this.clientRegistrationEnabled()) {
        this.types = [...ApplicationType.TYPES];

        // Filter types according to the allowed application type
        _.remove(this.types, (type: ApplicationType) => {
          return !this.Constants.application.types[type.value.toLowerCase()].enabled;
        });

        let web = _.find(this.types, (type: ApplicationType) => { return type === ApplicationType.WEB});

        this.selectType((web) ? ApplicationType.WEB : this.types[0]);
      } else {
        this.selectType(ApplicationType.SIMPLE);
      }
    }

    create() {
      this.ApplicationService.create(this.application).then((response) => {
        this.NotificationService.show('Application ' + this.application.name + ' has been created');
        this.$state.go('management.applications.application.general', {applicationId: response.data.id}, {reload: true});
      });
    }

    selectType(applicationType: ApplicationType) {
      this.selectedType = _.find(this.types, (type) => type.value === applicationType.value);

      if (this.selectedType.oauth) {
        this.application.settings = {
          oauth: _.merge({
            grant_types: this.selectedType.oauth.default_grant_types
          }, this.selectedType.configuration.oauth)
        };

        // Update response_types according to the selected grant type
        this.updateGrantTypes();
      } else {
        this.application.settings = {
          app: {}
        }
      }
    }

    reset() {
      this.application = {};
      this.$scope.formApplication.$setPristine();
    }

    clientRegistrationEnabled() {
      return this.Constants.application && this.Constants.application.registration && this.Constants.application.registration.enabled;
    }

    isOAuthClient() {
      return this.application.settings.oauth && (
        _.indexOf(this.application.settings.oauth.grant_types, GrantType.AUTHORIZATION_CODE.type) != -1 ||
        _.indexOf(this.application.settings.oauth.grant_types, GrantType.IMPLICIT.type) != -1);
    }

    updateGrantTypes() {
      this.application.settings.oauth.response_types =
        _.flatMap(this.application.settings.oauth.grant_types,
          (selected) => _.find(this.grantTypes, (grantType) => grantType.type === selected).response_types);
    }
  }
};

export default CreateApplicationComponent;
