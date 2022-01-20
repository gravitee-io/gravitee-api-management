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
import { StateParams, StateService } from '@uirouter/core';
import * as _ from 'lodash';

import { ApiService } from '../../../../../services/api.service';
import NotificationService from '../../../../../services/notification.service';

class ApiResponseTemplateController {
  private api: any;

  private selectedStatusCode: any;
  private templateKey: string;
  private formResponseTemplate: any;
  private creation = false;
  private keys: any;
  private templates: any;
  private selectedTemplateKey: any;

  constructor(
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private $rootScope: ng.IRootScopeService,
    private $scope,
    private $stateParams: StateParams,
    private $state: StateService,
  ) {
    'ngInject';

    this.creation = this.$stateParams.key === undefined;

    this.templateKey = this.$stateParams.key;

    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);

    this.templates = [];

    if (this.api.response_templates && this.api.response_templates[this.$stateParams.key]) {
      Object.keys(this.api.response_templates[this.$stateParams.key]).forEach((type) => {
        const template = this.api.response_templates[this.$stateParams.key][type];
        this.templates.push({
          type: type,
          status: template.status,
          body: template.body,
          headers: template.headers ? Object.keys(template.headers).map((name) => ({ name, value: template.headers[name] })) : [],
        });
      });
    }

    this.keys = [
      'DEFAULT',
      'API_KEY_MISSING',
      'API_KEY_INVALID',
      'QUOTA_TOO_MANY_REQUESTS',
      'RATE_LIMIT_TOO_MANY_REQUESTS',
      'REQUEST_CONTENT_LIMIT_TOO_LARGE',
      'REQUEST_CONTENT_LIMIT_LENGTH_REQUIRED',
      'REQUEST_TIMEOUT',
      'REQUEST_VALIDATION_INVALID',
      'RESOURCE_FILTERING_FORBIDDEN',
      'RESOURCE_FILTERING_METHOD_NOT_ALLOWED',
      'RBAC_FORBIDDEN',
      'RBAC_INVALID_USER_ROLES',
      'RBAC_NO_USER_ROLE',
      'OAUTH2_MISSING_SERVER',
      'OAUTH2_MISSING_HEADER',
      'OAUTH2_MISSING_ACCESS_TOKEN',
      'OAUTH2_INVALID_ACCESS_TOKEN',
      'OAUTH2_INVALID_SERVER_RESPONSE',
      'OAUTH2_INSUFFICIENT_SCOPE',
      'OAUTH2_SERVER_UNAVAILABLE',
      'HTTP_SIGNATURE_INVALID_SIGNATURE',
      'JWT_MISSING_TOKEN',
      'JWT_INVALID_TOKEN',
      'JSON_INVALID_PAYLOAD',
      'JSON_INVALID_FORMAT',
      'JSON_INVALID_RESPONSE_PAYLOAD',
      'JSON_INVALID_RESPONSE_FORMAT',
      'GATEWAY_INVALID_REQUEST',
      'GATEWAY_INVALID_RESPONSE',
      'GATEWAY_OAUTH2_ACCESS_DENIED',
      'GATEWAY_OAUTH2_SERVER_ERROR',
      'GATEWAY_OAUTH2_INVALID_CLIENT',
      'GATEWAY_MISSING_SECURITY_PROVIDER',
      'GATEWAY_POLICY_INTERNAL_ERROR',
      'GATEWAY_PLAN_UNRESOLVABLE',
      'GATEWAY_CLIENT_CONNECTION_ERROR',
      'GATEWAY_CLIENT_CONNECTION_TIMEOUT',
    ];

    // In case of a new response template, initialize with default media type
    if (this.creation) {
      this.addTemplate('*/*');
    }
  }

  onSelectedTemplateKey(key) {
    this.templateKey = key;
  }

  querySearchTemplateKey(query) {
    const keys = query ? this.keys.filter(this.createFilterForTemplateKey(query)) : this.keys;
    if (query && !_.includes(keys, query)) {
      this.selectedTemplateKey = query;
    }
    return keys;
  }

  createFilterForTemplateKey(query) {
    return function filterFn(state) {
      return _.includes(state.toLowerCase(), query.toLowerCase());
    };
  }

  addTemplate(type?: string) {
    this.templates.push({
      type: type,
      status: 400,
      headers: [],
    });
  }

  deleteTemplate(type) {
    _.remove(this.templates, (template: any) => template.type === type);
    this.formResponseTemplate.$setDirty();
  }

  update() {
    const apiResponseTemplates = this.api.response_templates || {};

    if (this.templates.length > 0) {
      apiResponseTemplates[this.templateKey] = _.mapValues(
        _.keyBy(this.templates, (template) => template.type),
        (template) => {
          return {
            status: template.status,
            headers: _.mapValues(_.keyBy(template.headers, 'name'), 'value'),
            body: template.body,
          };
        },
      );
      this.api.response_templates = apiResponseTemplates;
    } else {
      delete this.api.response_templates[this.templateKey];
    }

    this.ApiService.update(this.api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.onApiUpdate();
    });
  }

  onApiUpdate() {
    this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    this.NotificationService.show('Response template saved for key: ' + this.templateKey);
    this.$state.go('management.apis.detail.proxy.responsetemplates.list');
  }

  reset() {
    this.$state.reload();
  }
}

export default ApiResponseTemplateController;
