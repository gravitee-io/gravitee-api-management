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
import { deepClone } from '@gravitee/ui-components/src/lib/utils';
import { IPromise } from 'angular';
import { ApiService } from '../../../../services/api.service';
import { DebugApiService } from '../../../../services/debugApi.service';

export const propertyProviders = [
  {
    id: 'HTTP',
    name: 'Custom (HTTP)',
    schema: {
      type: 'object',
      properties: {
        method: {
          title: 'HTTP Method',
          description: 'HTTP method to invoke the endpoint.',
          type: 'string',
          default: 'GET',
          enum: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'CONNECT', 'OPTIONS', 'TRACE'],
        },
        url: {
          title: 'Http service URL',
          description: 'http://localhost',
          type: 'string',
          pattern: '^(http://|https://)',
        },
        headers: {
          type: 'array',
          title: 'Request Headers',
          items: {
            type: 'object',
            title: 'Header',
            properties: {
              name: {
                title: 'Name',
                type: 'string',
              },
              value: {
                title: 'Value',
                type: 'string',
              },
            },
          },
        },
        body: {
          title: 'Request body',
          type: 'string',
          'x-schema-form': {
            type: 'codemirror',
            codemirrorOptions: {
              lineWrapping: true,
              lineNumbers: true,
              allowDropFileTypes: true,
              autoCloseTags: true,
            },
          },
        },
        specification: {
          title: 'Transformation (JOLT Specification)',
          type: 'string',
          'x-schema-form': {
            type: 'codemirror',
            codemirrorOptions: {
              lineWrapping: true,
              lineNumbers: true,
              allowDropFileTypes: true,
              autoCloseTags: true,
              mode: 'javascript',
            },
          },
        },
      },
      required: ['url', 'specification'],
    },
    documentation:
      '= Custom (HTTP)\n\n=== How to ?\n\n 1. Set `Polling frequency interval` and `Time unit`\n2. Set the `HTTP service URL`\n 3. If the HTTP service doesn\'t return the expected output, add a JOLT `transformation` \n\n[source, json]\n----\n[\n  {\n    "key": 1,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 2,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 3,\n    "value": "https://south-asia.company.com/"\n  }\n]\n----\n',
  },
];

class ApiDesignController {
  private api: any;
  private readonlyPlans: any;
  private definition: any;
  private services: any;
  private propertyProviders = propertyProviders;

  constructor(
    private resolvedResources,
    private resolvedPolicies,
    private resolvedFlowSchemaForm,
    private $scope,
    private readonly ApiService: ApiService,
    private readonly debugApiService: DebugApiService,
    private NotificationService,
    private $rootScope,
    private $stateParams,
    private UserService,
  ) {
    'ngInject';
  }

  $onInit = () => {
    this.setApi(this.$scope.$parent.apiCtrl.api);
    this.ApiService.get(this.$stateParams.apiId).then((response) => {
      this.setApi(response.data);
    });

    if (!this.UserService.isUserHasPermissions(['api-plan-u'])) {
      this.readonlyPlans = true;
    }
  };

  setApi(api) {
    if (api !== this.api) {
      this.api = deepClone(api);
      this.definition = {
        name: this.api.name,
        version: this.api.version,
        flows: this.api.flows != null ? this.api.flows : [],
        resources: this.api.resources,
        plans: this.UserService.isUserHasPermissions(['api-plan-r', 'api-plan-u']) && this.api.plans != null ? this.api.plans : [],
        properties: this.api.properties,
        'flow-mode': this.api.flow_mode,
      };
      this.services = this.api.services;
    }
  }

  onSave(event) {
    const { definition, services } = event.detail;
    this.api.flows = definition.flows;
    this.api.plans = definition.plans;
    this.api.resources = definition.resources;
    this.api.properties = definition.properties;
    this.api.services = services;
    this.api.flow_mode = definition['flow-mode'];
    this.ApiService.update(this.api).then(() => {
      this.NotificationService.show('Design of api has been updated');
      event.target.saved();
      this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    });
  }

  public onDebug(event: { detail: { definition: any; services: any; request: any } }): IPromise<void> {
    const { definition, services, request } = event.detail;
    const debugApi = deepClone(this.api);
    debugApi.flows = definition.flows;
    debugApi.plans = definition.plans;
    debugApi.resources = definition.resources;
    debugApi.properties = definition.properties;
    debugApi.services = services;
    debugApi.flow_mode = definition['flow-mode'];

    const consolidatedRequest = {
      body: '',
      headers: {},
      ...request,
    };

    return this.debugApiService.debug(debugApi, consolidatedRequest).then(() => {
      this.NotificationService.show('Try it in progress...');
    });
  }
}

export default ApiDesignController;
