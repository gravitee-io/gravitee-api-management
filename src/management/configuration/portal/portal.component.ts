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
import PortalConfigService from '../../../services/portalConfig.service';
import { StateService } from '@uirouter/core';
import _ = require('lodash');
import CorsService from '../../../services/cors.service';
import ApiService from '../../../services/api.service';

const PortalSettingsComponent: ng.IComponentOptions = {
  bindings: {
    tags: '<',
    settings: '<',
  },
  template: require('./portal.html'),
  controller: function(
    NotificationService: NotificationService,
    PortalConfigService: PortalConfigService,
    CorsService: CorsService,
    ApiService: ApiService,
    $state: StateService,
    Constants: any
  ) {
    'ngInject';
    this.methods = CorsService.getHttpMethods();
    this.headers = ApiService.defaultHttpHeaders();
    this.searchHeaders = null;
    this.providedConfigurationMessage = 'Configuration provided by the system';

    this.$onInit = () => {
      this.settings.api.labelsDictionary = this.settings.api.labelsDictionary || [];
      this.settings.cors.allowOrigin = this.settings.cors.allowOrigin || [];
      this.settings.cors.allowHeaders = this.settings.cors.allowHeaders || [];
      this.settings.cors.allowMethods = this.settings.cors.allowMethods || [];
      this.settings.cors.exposedHeaders = this.settings.cors.exposedHeaders || [];
      this.settings.authentication.localLogin.enabled = (this.settings.authentication.localLogin.enabled || !this.hasIdpDefined());
    };

    this.save = () => {
      PortalConfigService.save(this.settings).then( (response) => {
        // We have to manually set this property because lodash's merge do not handle well the case of label deletion
        Constants.api.labelsDictionary = response.data.api.labelsDictionary;
        _.merge(Constants, response.data);
        NotificationService.show('Configuration saved');
        this.reset();
      });
    };

    this.reset = () => {
      $state.reload();
    };

    this.hasIdpDefined = () => {
      return this.settings.authentication.google.clientId ||
       this.settings.authentication.github.clientId ||
       this.settings.authentication.oauth2.clientId;
    };

    this.toggleDocType = () => {
      if (!this.settings.openAPIDocViewer.openAPIDocType.swagger.enabled) {
        this.settings.openAPIDocViewer.openAPIDocType.defaultType = 'Redoc';
      }
      if (!this.settings.openAPIDocViewer.openAPIDocType.redoc.enabled) {
        this.settings.openAPIDocViewer.openAPIDocType.defaultType = 'Swagger';
      }
    };

    this.isReadonlySetting = (property: string): boolean => {
      return PortalConfigService.isReadonly(this.settings, property);
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

export default PortalSettingsComponent;
