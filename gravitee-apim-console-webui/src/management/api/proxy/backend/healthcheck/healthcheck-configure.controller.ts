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
import angular = require('angular');
import _ = require('lodash');

import '@gravitee/ui-components/wc/gv-cron-editor';
import '@gravitee/ui-components/wc/gv-expression-language';
import { ApiService } from '../../../../../services/api.service';
import NotificationService from '../../../../../services/notification.service';

class ApiHealthCheckConfigureController {
  private api: any;
  private healthcheck: { enabled: boolean; inherit: boolean; schedule?: string; steps?: any[]; response?: any };
  private httpMethods: string[];
  private endpoint: any;
  private endpointToDisplay: any;
  private rootHealthcheckEnabled: boolean;
  private spelGrammar: { dictionaries: any; properties: any; _types: any; _enums: any };
  private hasHealthCheck: boolean;

  constructor(
    private ApiService: ApiService,
    private NotificationService: NotificationService,
    private $mdDialog: angular.material.IDialogService,
    private $scope,
    private $state,
    private $stateParams,
    private $rootScope,
    private $window,
    private resolvedSpelGrammar: any,
  ) {
    'ngInject';

    this.spelGrammar = {
      dictionaries: resolvedSpelGrammar.data.dictionaries,
      properties: resolvedSpelGrammar.data.properties,
      _types: resolvedSpelGrammar.data._types,
      _enums: resolvedSpelGrammar.data._enums,
    };
    this.api = this.$scope.$parent.apiCtrl.api;
    this.$scope.$on('apiChangeSuccess', (event, args) => {
      this.api = args.api;
    });

    if (this.$stateParams.endpointName !== undefined) {
      // Health-check for specific endpoint
      const group: any = _.find(this.api.proxy.groups, { name: $stateParams.groupName });
      this.endpoint = _.find(group.endpoints, { name: $stateParams.endpointName });
      this.rootHealthcheckEnabled = this.api.services && this.api.services['health-check'] && this.api.services['health-check'].enabled;

      if (!this.endpoint.healthcheck && this.rootHealthcheckEnabled) {
        this.healthcheck = { enabled: true, inherit: true };
      } else {
        this.healthcheck = this.endpoint.healthcheck;
      }
      this.endpointToDisplay = this.endpoint;
      // FIXME: https://github.com/gravitee-io/issues/issues/6437
      this.hasHealthCheck =
        this.endpointToDisplay != null &&
        (this.endpointToDisplay.type?.toLowerCase() === 'http' || this.endpointToDisplay.type?.toLowerCase() === 'grpc');
    } else {
      this.hasHealthCheck = true;
      this.healthcheck = this.api.services && this.api.services['health-check'];
    }
    this.healthcheck = this.healthcheck || { enabled: false, inherit: false, schedule: '*/1 * * * * *' };
    const inherit = this.endpoint !== undefined && this.healthcheck.inherit;
    const enabled = this.healthcheck.enabled;

    if (inherit) {
      this.healthcheck = _.cloneDeep((this.api.services && this.api.services['health-check']) || { enabled: false });
    }

    this.healthcheck.inherit = inherit;
    this.healthcheck.enabled = enabled;

    this.httpMethods = ['GET', 'POST', 'PUT'];

    this.initState();
  }

  initState() {
    if (this.healthcheck.steps === undefined) {
      this.healthcheck.steps = [];
    }

    if (this.healthcheck.steps[0] === undefined) {
      this.healthcheck.steps[0] = {
        request: {
          headers: [],
        },
        response: {
          assertions: [],
        },
      };
    }
  }

  openMenu($mdOpenMenu, ev) {
    $mdOpenMenu(ev);
  }

  addHTTPHeader() {
    if (this.healthcheck.steps[0].request.headers === undefined) {
      this.healthcheck.steps[0].request.headers = [];
    }

    this.healthcheck.steps[0].request.headers.push({ name: '', value: '' });
  }

  removeHTTPHeader(idx) {
    if (this.healthcheck.steps[0].request.headers !== undefined) {
      this.healthcheck.steps[0].request.headers.splice(idx, 1);
    }
  }

  addAssertion() {
    if (this.healthcheck.steps[0].response === undefined) {
      this.healthcheck.response = {
        assertions: [''],
      };
    } else {
      this.healthcheck.steps[0].response.assertions.push('');
    }
  }

  removeAssertion(idx) {
    if (this.healthcheck.steps[0].response !== undefined) {
      this.healthcheck.steps[0].response.assertions.splice(idx, 1);
    }
  }

  buildRequest() {
    let request = '';

    request += ((this.healthcheck.steps && this.healthcheck.steps[0].request.method) || '{method}') + ' ';

    if (this.healthcheck.steps && this.healthcheck.steps[0].request.fromRoot) {
      if (this.endpointToDisplay) {
        try {
          request += new URL(this.endpointToDisplay.target).origin;
        } catch (e) {
          request += this.endpointToDisplay.target;
        }
      } else {
        request += '{endpoint}';
      }
      request += (this.healthcheck.steps && this.healthcheck.steps[0].request.path) || '/{path}';
    } else {
      request += this.endpointToDisplay ? this.endpointToDisplay.target : '{endpoint}';
      request += (this.healthcheck.steps && this.healthcheck.steps[0].request.path) || '/{path}';
    }

    return request;
  }

  showAssertionInformation() {
    this.$mdDialog.show({
      controller: 'DialogAssertionInformationController',
      controllerAs: 'ctrl',
      template: require('./assertion.dialog.html'),
      parent: angular.element(document.body),
      clickOutsideToClose: true,
    });
  }

  backToEndpointConfiguration() {
    this.$state.go('management.apis.detail.proxy.endpoint', {
      groupName: this.$stateParams.groupName,
      endpointName: this.$stateParams.endpointName,
    });
  }

  backToHealthcheck() {
    const query = JSON.parse(this.$window.localStorage.lastHealthCheckQuery);
    this.$state.go('management.apis.detail.proxy.healthcheck.visualize', {
      page: query.page,
      size: query.size,
      from: query.from,
      to: query.to,
    });
  }

  updateSchedule(event) {
    if (event.target.valid) {
      this.healthcheck.schedule = event.target.value;
      this.$scope.formApiHealthCheckTrigger.$invalid = false;
    } else {
      this.$scope.formApiHealthCheckTrigger.$invalid = true;
    }
  }

  cannotUpdate() {
    return (
      (this.healthcheck.inherit === false || this.healthcheck.inherit == null) &&
      this.healthcheck.enabled === true &&
      (this.$scope.formApiHealthCheckTrigger.$invalid ||
        this.$scope.formApiHealthCheckResponse.$invalid ||
        this.$scope.formApiHealthCheckRequest.$invalid)
    );
  }

  update() {
    if (this.endpoint !== undefined) {
      this.endpoint.healthcheck = this.healthcheck;
    } else {
      // inherit is only available for health check on endpoint
      delete this.healthcheck.inherit;
      // health-check is disabled, set dummy values
      if (this.healthcheck.enabled === false) {
        delete this.healthcheck.steps;
        this.healthcheck.schedule = '*/1 * * * * *';
      }
      this.api.services['health-check'] = this.healthcheck;
    }
    this.ApiService.update(this.api).then((updatedApi) => {
      this.api = updatedApi.data;
      this.initState();
      this.api.etag = updatedApi.headers('etag');
      this.$scope.formApiHealthCheckTrigger.$setPristine();
      this.$scope.formApiHealthCheckRequest.$setPristine();
      this.$scope.formApiHealthCheckResponse.$setPristine();
      this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });

      const notification = this.endpoint
        ? `Health-check configuration for endpoint [${this.endpoint.name}] has been updated`
        : 'Global health-check configuration  has been updated';

      this.NotificationService.show(notification);
    });
  }
}

export default ApiHealthCheckConfigureController;
