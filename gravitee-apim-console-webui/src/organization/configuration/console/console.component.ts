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
import { StateService } from '@uirouter/core';
import * as _ from 'lodash';

import { ApiService } from '../../../services/api.service';
import ConsoleSettingsService from '../../../services/consoleSettings.service';
import CorsService from '../../../services/cors.service';
import NotificationService from '../../../services/notification.service';

const ConsoleSettingsComponent: ng.IComponentOptions = {
  bindings: {
    tags: '<',
    settings: '<',
  },
  template: require('./console.html'),
  controller: function (
    NotificationService: NotificationService,
    ConsoleSettingsService: ConsoleSettingsService,
    CorsService: CorsService,
    ApiService: ApiService,
    $state: StateService,
    Constants: any,
  ) {
    'ngInject';
    this.methods = CorsService.getHttpMethods();
    this.headers = ApiService.defaultHttpHeaders();
    this.searchHeaders = null;
    this.providedConfigurationMessage = 'Configuration provided by the system';

    this.$onInit = () => {
      this.settings.cors.allowOrigin = this.settings.cors.allowOrigin || [];
      this.settings.cors.allowHeaders = this.settings.cors.allowHeaders || [];
      this.settings.cors.allowMethods = this.settings.cors.allowMethods || [];
      this.settings.cors.exposedHeaders = this.settings.cors.exposedHeaders || [];
      this.settings.authentication.localLogin.enabled = this.settings.authentication.localLogin.enabled || !this.hasIdpDefined();
      this.initialSettings = _.cloneDeep(this.settings);
    };

    this.save = () => {
      ConsoleSettingsService.save(this.settings).then((response) => {
        _.merge(Constants.org.settings, response.data);
        NotificationService.show('Configuration saved');
        $state.reload();
      });
    };

    this.reset = () => {
      this.settings = _.cloneDeep(this.initialSettings);
      this.formSettings.$setPristine();
    };

    this.hasIdpDefined = () => {
      return (
        this.settings.authentication.google.clientId ||
        this.settings.authentication.github.clientId ||
        this.settings.authentication.oauth2.clientId
      );
    };

    this.isReadonlySetting = (property: string): boolean => {
      return ConsoleSettingsService.isReadonly(this.settings, property);
    };

    this.controlAllowOrigin = (chip, index) => {
      CorsService.controlAllowOrigin(chip, index, this.settings.cors.allowOrigin);
    };

    this.isRegexValid = () => {
      return CorsService.isRegexValid(this.settings.cors.allowOrigin);
    };

    this.querySearchHeaders = (query) => {
      return CorsService.querySearchHeaders(query, this.headers);
    };
  },
};

export default ConsoleSettingsComponent;
