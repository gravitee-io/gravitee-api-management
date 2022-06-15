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
import * as _ from 'lodash';

import { ConfigureLoggingDialogController } from './configure-logging.dialog';

import { ApiService } from '../../../../services/api.service';
import NotificationService from '../../../../services/notification.service';

import '@gravitee/ui-components/wc/gv-switch';
import '@gravitee/ui-components/wc/gv-expression-language';
import '@gravitee/ui-components/wc/gv-option';
import '@gravitee/ui-components/wc/gv-button';

class ApiLoggingConfigurationController {
  private initialApi: any;
  private api: any;
  private formLogging: any;
  private maxDuration: any;
  private enabled: boolean;
  private defaultLogging = { mode: 'CLIENT_PROXY', content: 'HEADERS_PAYLOADS', scope: 'REQUEST_RESPONSE' };

  constructor(
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private $stateParams,
    private $rootScope,
    private $scope,
    private Constants,
    private spelGrammar: any,
  ) {
    'ngInject';

    this.initialApi = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
    this.spelGrammar = spelGrammar.data;
    this.enabled = this.api.proxy.logging && this.api.proxy.logging.mode !== 'NONE';
    this.initLogging(this.enabled);
    this.maxDuration = Constants.org.settings.logging.maxDurationMillis;

    this.$scope.loggingModes = [
      {
        title: 'Client & proxy',
        id: 'CLIENT_PROXY',
        icon: 'home:earth',
        description: 'to log content for the client and the gateway',
      },
      {
        title: 'Client only',
        id: 'CLIENT',
        icon: 'communication:shield-user',
        description: 'to log content between the client and the gateway',
      },
      {
        title: 'Proxy only',
        id: 'PROXY',
        icon: 'communication:shield-thunder',
        description: 'to log content between the gateway and the backend',
      },
    ];

    this.$scope.contentModes = [
      {
        title: 'Headers & payloads',
        id: 'HEADERS_PAYLOADS',
        icon: 'finance:selected-file',
        description: 'to log headers and payloads',
      },
      {
        title: 'Headers only',
        id: 'HEADERS',
        icon: 'finance:file',
        description: 'to log headers without payloads',
      },
      {
        title: 'Payloads only',
        id: 'PAYLOADS',
        icon: 'communication:clipboard-list',
        description: 'to log payloads without headers',
      },
    ];

    this.$scope.scopeModes = [
      {
        title: 'Request & Response',
        id: 'REQUEST_RESPONSE',
        icon: 'navigation:exchange',
        description: 'to log request and response',
      },
      {
        title: 'Request only',
        id: 'REQUEST',
        icon: 'navigation:right-2',
        description: 'to log request content only',
      },
      {
        title: 'Response only',
        id: 'RESPONSE',
        icon: 'navigation:left-2',
        description: 'to log response content only',
      },
    ];

    this.$scope.$on('apiChangeSuccess', (event, args) => {
      this.api = args.api;
      this.enabled = this.api.proxy.logging.mode !== 'NONE';
    });
  }

  initLogging(enabled: boolean) {
    if (this.api.proxy.logging == null) {
      this.api.proxy.logging = {};
    }
    if (enabled) {
      const tmp = { ...this.defaultLogging, ...this.initialApi.proxy.logging };
      const { mode, scope, content } = tmp;
      this.api.proxy.logging.mode = mode !== 'NONE' ? mode : this.defaultLogging.mode;
      this.api.proxy.logging.scope = scope !== 'NONE' ? scope : this.defaultLogging.scope;
      this.api.proxy.logging.content = content !== 'NONE' ? content : this.defaultLogging.content;
    } else {
      this.api.proxy.logging.mode = 'NONE';
      this.api.proxy.logging.scope = 'NONE';
      this.api.proxy.logging.content = 'NONE';
    }
  }

  switchMode({ detail }: { detail: boolean }) {
    this.initLogging(detail);
  }

  update() {
    if (!this.enabled) {
      this.api.proxy.logging.mode = 'NONE';
    }
    if (this.api.proxy.logging.condition != null && this.api.proxy.logging.condition.trim() === '') {
      delete this.api.proxy.logging.condition;
    }

    this.ApiService.update(this.api).then((updatedApi) => {
      this.NotificationService.show('Logging configuration has been updated');
      this.api = updatedApi.data;
      this.api.etag = updatedApi.headers('etag');
      this.initialApi = _.cloneDeep(updatedApi.data);
      this.$scope.formLogging.$setPristine();
      this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    });
  }

  reset() {
    this.api = _.cloneDeep(this.initialApi);
    this.enabled = this.api.proxy.logging.mode !== 'NONE';
    this.initLogging(this.enabled);
    if (this.$scope.formLogging) {
      this.$scope.formLogging.$setPristine();
      this.$scope.formLogging.$setUntouched();
    }
  }

  clearCondition() {
    this.api.proxy.logging.condition = '';
    this.$scope.formLogging.$setDirty();
  }

  hasCondition() {
    return this.api.proxy.logging.condition != null && this.api.proxy.logging.condition !== '';
  }

  showConditionEditor() {
    this.$mdDialog
      .show({
        controller: ConfigureLoggingDialogController,
        controllerAs: '$ctrl',
        template: require('./configure-logging.dialog.html'),
        clickOutsideToClose: true,
        resolve: {
          subscribers: ($stateParams, ApiService: ApiService) => {
            'ngInject';
            return ApiService.getSubscribers($stateParams.apiId).then((response) => response.data);
          },
          plans: ($stateParams, ApiService: ApiService) => {
            'ngInject';
            return ApiService.getApiPlans($stateParams.apiId, 'published,deprecated').then((response) => response.data);
          },
        },
      })
      .then(
        (condition) => {
          if (condition) {
            this.api.proxy.logging.condition = `{${condition}}`;
            this.$scope.formLogging.$setDirty();
          }
        },
        () => {
          // Cancel of the dialog
        },
      );
  }
}

export default ApiLoggingConfigurationController;
