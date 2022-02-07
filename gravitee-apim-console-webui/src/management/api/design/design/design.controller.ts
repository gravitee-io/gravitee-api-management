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
import { IIntervalService, IPromise, ITimeoutService } from 'angular';

import { Event } from '../../../../entities/event/event';
import { ApiService } from '../../../../services/api.service';
import { DebugApiService } from '../../../../services/debugApi.service';
import { EventService } from '../../../../services/event.service';
import NotificationService from '../../../../services/notification.service';

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
        useSystemProxy: {
          title: 'Use system proxy',
          description: 'Use the system proxy configured by your administrator.',
          type: 'boolean',
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

export const configurationInformation =
  'By default, the selection of a flow is based on the operator defined in the flow itself. This operator allows either to select a flow when the path matches exactly, or when the start of the path matches. The "Best match" option allows you to select the flow from the path that is closest.';

export const providersTitleMap = propertyProviders.reduce((map, provider) => {
  map[provider.id] = provider.name;
  return map;
}, {});

export const providersEnum = Object.keys(providersTitleMap);

export const dynamicPropertySchema = {
  properties: {
    enabled: {
      type: 'boolean',
      title: 'Enabled',
      description: ' This service is requiring an API deployment. Do not forget to deploy API to start dynamic-properties service.',
    },
    trigger: {
      type: 'object',
      properties: {
        rate: {
          type: 'integer',
          title: 'Polling frequency interval',
        },
        unit: {
          type: 'string',
          title: 'Time unit',
          enum: ['SECONDS', 'MINUTES', 'HOURS'],
        },
      },
      required: ['rate', 'unit'],
    },
    provider: {
      type: 'string',
      title: 'Provider type',
      enum: providersEnum,
      default: providersEnum[0],
      'x-schema-form': {
        titleMap: providersTitleMap,
      },
    },
  },
  required: ['trigger', 'provider'],
};

class ApiDesignController {
  private api: any;
  private readonlyPlans: any;
  private definition: any;
  private services: any;
  private propertyProviders = propertyProviders;
  private debugResponse: {
    isLoading: boolean;
    response?: unknown;
    request?: unknown;
  };

  constructor(
    private resolvedResources,
    private resolvedPolicies,
    private resolvedFlowSchemaForm,
    private $scope,
    private readonly ApiService: ApiService,
    private readonly debugApiService: DebugApiService,
    private readonly eventService: EventService,
    private NotificationService: NotificationService,
    private $rootScope,
    private $stateParams,
    private $interval: IIntervalService,
    private $timeout: ITimeoutService,
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
        flow_mode: this.api.flow_mode,
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
    this.api.flow_mode = definition.flow_mode;
    this.ApiService.update(this.api).then((updatedApi) => {
      this.NotificationService.show('Design of api has been updated');
      this.setApi(updatedApi.data);
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
    debugApi.flow_mode = definition.flow_mode;

    const headersAsMap = (request.headers ?? [])
      .filter((header) => !!header.value)
      .reduce((acc, current) => {
        acc[current.name] = current.value;
        return acc;
      }, {});

    const consolidatedRequest = {
      body: request.body,
      path: request.path,
      method: request.method,
      headers: headersAsMap,
    };

    this.debugResponse = {
      isLoading: true,
    };

    return this.debugApiService
      .debug(debugApi, consolidatedRequest)
      .then((event) => this.pollAndGetSuccessfulEvent(event, 10))
      .then((event) => {
        this.debugResponse = {
          isLoading: false,
          response: JSON.parse(event.payload).response,
          request: consolidatedRequest,
        };
      })
      .catch((error) => {
        this.NotificationService.showError(error, 'Unable to try the request, please try again');
        this.debugResponse = {
          isLoading: false,
          request: consolidatedRequest,
        };
      });
  }

  private pollAndGetSuccessfulEvent(debugEvent: Event, retryCount: number): IPromise<Event> {
    if (retryCount <= 0) {
      throw new Error('Max number of retry reached');
    }

    return this.$timeout(() => {
      return this.eventService.findById(this.api.id, debugEvent.id);
    }, 1000).then((event) => {
      if (event.properties.api_debug_status === 'SUCCESS') {
        return event;
      } else {
        return this.pollAndGetSuccessfulEvent(debugEvent, retryCount - 1);
      }
    });
  }
}

export default ApiDesignController;
