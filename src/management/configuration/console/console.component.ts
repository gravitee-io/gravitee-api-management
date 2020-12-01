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
import NotificationService from '../../../services/notification.service';
import ConsoleConfigService from '../../../services/consoleConfig.service';
import { StateService } from '@uirouter/core';
import CorsService from '../../../services/cors.service';
import ApiService from '../../../services/api.service';
import _ = require('lodash');

const ConsoleSettingsComponent: ng.IComponentOptions = {
  bindings: {
    tags: '<'
  },
  template: require('./console.html'),
  controller: function(
    NotificationService: NotificationService,
    ConsoleConfigService: ConsoleConfigService,
    CorsService: CorsService,
    ApiService: ApiService,
    $state: StateService,
    Constants: any
  ) {
    'ngInject';
    this.settings = _.cloneDeep(Constants.org.settings);
    this.methods = CorsService.getHttpMethods();
    this.headers = ApiService.defaultHttpHeaders();
    this.searchHeaders = null;
    this.providedConfigurationMessage = 'Configuration provided by the system';

    this.$onInit = () => {
      this.settings.cors.allowOrigin = this.settings.cors.allowOrigin || [];
      this.settings.cors.allowHeaders = this.settings.cors.allowHeaders || [];
      this.settings.cors.allowMethods = this.settings.cors.allowMethods || [];
      this.settings.cors.exposedHeaders = this.settings.cors.exposedHeaders || [];
      this.settings.authentication.localLogin.enabled = (this.settings.authentication.localLogin.enabled || !this.hasIdpDefined());
    };

    this.save = () => {
      ConsoleConfigService.save(this.settings).then( (response) => {
        // We have to manually set this property because lodash's merge do not handle well the case of label deletion
        Constants.org.settings.cors.allowOrigin = response.data.cors.allowOrigin;
        Constants.org.settings.cors.allowHeaders = response.data.cors.allowHeaders;
        Constants.org.settings.cors.allowMethods = response.data.cors.allowMethods;
        Constants.org.settings.cors.exposedHeaders = response.data.cors.exposedHeaders;
        _.merge(Constants.org.settings, response.data);
        NotificationService.show('Configuration saved');
        this.reset();
        $state.reload();
      });
    };

    this.reset = () => {
      this.settings = _.cloneDeep(Constants.org.settings);
      this.formSettings.$setPristine();
    };

    this.hasIdpDefined = () => {
      return this.settings.authentication.google.clientId ||
        this.settings.authentication.github.clientId ||
        this.settings.authentication.oauth2.clientId;
    };

    this.isReadonlySetting = (property: string): boolean => {
      return ConsoleConfigService.isReadonly(this.settings, property);
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
  }
};

export default ConsoleSettingsComponent;
