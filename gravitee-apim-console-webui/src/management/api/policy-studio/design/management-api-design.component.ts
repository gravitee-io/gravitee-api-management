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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, EMPTY, interval, Subject, timer } from 'rxjs';
import { catchError, filter, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { cloneDeep } from 'lodash';

import { PolicyService } from '../../../../services-ngx/policy.service';
import { Organization } from '../../../../entities/organization/organization';
import { PolicyListItem } from '../../../../entities/policy';
import { Flow } from '../../../../entities/flow/flow';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiService } from '../../../../services-ngx/api.service';
import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ResourceService } from '../../../../services-ngx/resource.service';
import { ResourceListItem } from '../../../../entities/resource/resourceListItem';
import { DebugApiService } from '../../../../services-ngx/debug-api.service';
import { EventService } from '../../../../services-ngx/event.service';
import { Services } from '../../../../entities/services';
import { ApiPlan, ApiProperty, ApiResource } from '../../../../entities/api';
import { ApiFlowSchema } from '../../../../entities/flow/apiFlowSchema';
import { FlowService } from '../../../../services-ngx/flow.service';
import { SpelService } from '../../../../services-ngx/spel.service';
import { StateParams } from '@uirouter/angularjs';
import { Location } from '@angular/common';

interface DefinitionVM {
  name: string;
  version: string;
  'flow-mode': 'DEFAULT' | 'BEST_MATCH';
  flows: Flow[];
  resources: ApiResource[];
  plans: ApiPlan[];
  properties: ApiProperty[];
}

const PROPERTY_PROVIDERS = [
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
const PROPERTY_PROVIDER_TITLES = PROPERTY_PROVIDERS.reduce((map, provider) => {
  map[provider.id] = provider.name;
  return map;
}, {} as Record<string, string>);
const PROPERTY_PROVIDER_IDS = Object.keys(PROPERTY_PROVIDER_TITLES);
const DYNAMIC_PROPERTY_SCHEMA = {
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
      enum: PROPERTY_PROVIDER_IDS,
      default: PROPERTY_PROVIDER_IDS[0],
      'x-schema-form': {
        titleMap: PROPERTY_PROVIDER_TITLES,
      },
    },
  },
  required: ['trigger', 'provider'],
};

@Component({
  selector: 'management-api-design',
  template: require('./management-api-design.component.html'),
  styles: [require('./management-api-design.component.scss')],
  providers: [
    {
      provide: 'FlowService',
      useExisting: FlowService,
    },
    {
      provide: 'PolicyService',
      useExisting: PolicyService,
    },
    {
      provide: 'ResourceService',
      useExisting: ResourceService,
    },
    {
      provide: 'SpelService',
      useExisting: SpelService,
    },
  ],
})
export class ManagementApiDesignComponent implements OnInit, OnDestroy {
  isLoading = true;

  organization: Organization;
  definition: DefinitionVM;
  services: Services;

  apiFlowSchema: ApiFlowSchema;
  policies: PolicyListItem[];
  resourceTypes: ResourceListItem[];
  readonlyPlans = false;
  configurationInformation =
    'By default, the selection of a flow is based on the operator defined in the flow itself. This operator allows either to select a flow when the path matches exactly, or when the start of the path matches. The "Best match" option allows you to select the flow from the path that is closest.';
  propertyProviders = PROPERTY_PROVIDERS;
  dynamicPropertySchema = DYNAMIC_PROPERTY_SCHEMA;
  debugResponse: {
    isLoading: boolean;
    response?: unknown;
    request?: unknown;
  };
  tabId: string;

  private api: any;
  private unsubscribe$ = new Subject<boolean>();

  constructor(
    private readonly resourceService: ResourceService,
    private readonly policyService: PolicyService,
    private readonly apiService: ApiService,
    private readonly debugApiService: DebugApiService,
    private readonly eventService: EventService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly location: Location,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
  ) {}

  ngOnInit(): void {
    console.log(this.ajsStateParams);

    this.location.onUrlChange((a) => {
      console.log('aa', a);
    });

    this.tabId = this.ajsStateParams.psPage ?? 'design';

    combineLatest([
      this.apiService.getFlowSchemaForm(),
      this.policyService.list({ expandSchema: true, expandIcon: true }),
      this.apiService.get(this.ajsStateParams.apiId),
      this.resourceService.list({ expandSchema: true, expandIcon: true }),
    ])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([flowSchema, policies, api, resources]) => {
          this.apiFlowSchema = flowSchema;
          this.policies = policies;
          this.resourceTypes = resources;
          this.api = api;

          this.definition = {
            name: api.name,
            version: api.version,
            flows: api.flows ?? [],
            resources: api.resources,
            plans: (this.permissionService.hasAnyMatching(['api-plan-r', 'api-plan-u']) ? api.plans : []) ?? [],
            properties: api.properties,
            'flow-mode': api.flow_mode,
          };
          this.services = api.services;

          if (!this.permissionService.hasAnyMatching(['api-plan-u'])) {
            this.readonlyPlans = true;
          }

          this.isLoading = false;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSave({ definition, services }: { definition: DefinitionVM; services: Services }) {
    this.api.flows = definition.flows;
    this.api.plans = definition.plans;
    this.api.resources = definition.resources;
    this.api.properties = definition.properties;
    this.api.services = services;
    this.api.flow_mode = definition['flow-mode'];
    this.apiService
      .update(this.api)
      .pipe(
        tap(() => {
          this.snackBarService.success('Design of api successfully updated!');
        }),
      )
      .subscribe(() => {
        this.ngOnInit();
      });
  }

  public onDebug({ definition, services, request }: { definition: DefinitionVM; services: Services; request: any }): void {
    const debugApi = cloneDeep(this.api);
    debugApi.flows = definition.flows;
    debugApi.plans = definition.plans;
    debugApi.resources = definition.resources;
    debugApi.properties = definition.properties;
    debugApi.services = services;
    debugApi.flow_mode = definition['flow-mode'];

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

    const maxPollingTime$ = timer(10000);
    this.debugApiService
      .debug(debugApi, consolidatedRequest)
      .pipe(
        // Poll each 1s to find success event. Stops after 10 seconds
        switchMap((debugEvent) => interval(1000).pipe(switchMap(() => this.eventService.findById(this.api.id, debugEvent.id)))),
        takeUntil(maxPollingTime$),
        takeUntil(this.unsubscribe$),
        filter((event) => event.properties.api_debug_status === 'SUCCESS'),
        take(1),
        catchError(() => {
          this.snackBarService.error('Unable to try the request, please try again');
          this.debugResponse = {
            isLoading: false,
            request: consolidatedRequest,
          };
          return EMPTY;
        }),
      )
      .subscribe((event) => {
        this.debugResponse = {
          isLoading: false,
          response: JSON.parse(event.payload).response,
          request: consolidatedRequest,
        };
      });
  }
}
