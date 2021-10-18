import { StateService } from '@uirouter/core';

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
// tslint:disable-next-line:no-var-requires
require('@gravitee/ui-components/wc/gv-policy-studio');

class ApiPolicyStudioController {
  private studio: any;
  private api: any;
  private CATEGORY_POLICY = ['security', 'performance', 'transformation', 'others'];

  constructor(
    private resolvedResources,
    private resolvedPolicies,
    private resolvedFlowSchema,
    private resolvedConfigurationSchema,
    private PolicyService,
    private ResourceService,
    private $scope,
    private ApiService,
    private SpelService,
    private NotificationService,
    private $rootScope,
    private $stateParams,
    private $location,
    private UserService,
  ) {
    'ngInject';
  }

  $onInit = () => {
    const propertyProviders = [
      {
        id: 'HTTP',
        name: 'Custom (HTTP)',
        schema: {
          type: 'object',
          properties: {
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
          required: ['url'],
        },
        documentation:
          '= Custom (HTTP)\n\n=== How to ?\n\n 1. Set `Polling frequency interval` and `Time unit`\n2. Set the `HTTP service URL`\n 3. If the HTTP service doesn\'t return the expected output, add a JOLT `transformation` \n\n[source, json]\n----\n[\n  {\n    "key": 1,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 2,\n    "value": "https://north-europe.company.com/"\n  },\n  {\n    "key": 3,\n    "value": "https://south-asia.company.com/"\n  }\n]\n----\n',
      },
    ];

    const providersTitleMap = propertyProviders.reduce((map, provider) => {
      map[provider.id] = provider.name;
      return map;
    }, {});

    const providersEnum = Object.keys(providersTitleMap);
    const dynamicPropertySchema = {
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

    this.resolvedPolicies.data.sort((a, b) => {
      if (a.category == null) {
        a.category = this.CATEGORY_POLICY[3];
      }
      if (b.category == null) {
        b.category = this.CATEGORY_POLICY[3];
      }
      if (a.category === b.category) {
        return 0;
      }
      const aKind = this.CATEGORY_POLICY.indexOf(a.category);
      if (aKind === -1) {
        return 1;
      }
      const bKind = this.CATEGORY_POLICY.indexOf(b.category);
      if (bKind === -1) {
        return -1;
      }
      return aKind < bKind ? -1 : 1;
    });
    this.studio = document.querySelector('gv-policy-studio');
    this.setApi(this.$scope.$parent.apiCtrl.api);
    this.ApiService.get(this.$stateParams.apiId).then((response) => {
      this.setApi(response.data);
    });

    this.studio.setAttribute('tab-id', this.$location.hash());
    let selectedFlows = null;
    const flowsParam = this.$location.search().flows;
    if (typeof flowsParam === 'string') {
      selectedFlows = [flowsParam];
    } else if (Array.isArray(flowsParam)) {
      selectedFlows = flowsParam;
    }
    this.studio.setAttribute('selected-flows-id', JSON.stringify(selectedFlows));
    this.studio.setAttribute('resource-types', JSON.stringify(this.resolvedResources.data));
    this.studio.setAttribute('policies', JSON.stringify(this.resolvedPolicies.data));
    this.studio.setAttribute('flow-schema', JSON.stringify(this.resolvedFlowSchema.data));
    this.studio.setAttribute('configuration-schema', JSON.stringify(this.resolvedConfigurationSchema.data));
    this.studio.setAttribute(
      'configuration-information',
      'By default, the selection of a flow is based on the operator defined in the flow itself. This operator allows either to select a flow when the path matches exactly, or when the start of the path matches. The "Best match" option allows you to select the flow from the path that is closest.',
    );
    this.studio.setAttribute('property-providers', JSON.stringify(propertyProviders));
    this.studio.setAttribute('dynamic-property-schema', JSON.stringify(dynamicPropertySchema));
    this.studio.setAttribute('flows-title', 'API Flows');
    this.studio.setAttribute('has-properties', 'true');
    this.studio.setAttribute('has-resources', 'true');
    this.studio.setAttribute('has-policy-filter', 'true');
    this.studio.setAttribute('sortable', 'true');
    this.studio.setAttribute('can-add', 'true');
    if (!this.UserService.isUserHasPermissions(['api-plan-u'])) {
      this.studio.setAttribute('readonly-plans', 'true');
    }
    this.studio.setAttribute('has-plans', 'true');
    this.studio.addEventListener('gv-policy-studio:save', this.onSave.bind(this));
    this.studio.addEventListener('gv-policy-studio:select-flows', this.onSelectFlows.bind(this));
    this.studio.addEventListener('gv-policy-studio:change-tab', this.onChangeTab.bind(this));
    this.studio.addEventListener('gv-policy-studio:fetch-documentation', this.fetchPolicyDocumentation.bind(this));
    this.studio.addEventListener('gv-resources:fetch-documentation', this.fetchResourceDocumentation.bind(this));
    this.studio.addEventListener('gv-expression-language:ready', this.fetchSpelGrammar.bind(this));
  };

  setApi(api) {
    if (api !== this.api) {
      this.api = api;
      this.studio.definition = {
        name: this.api.name,
        version: this.api.version,
        flows: this.api.flows != null ? this.api.flows : [],
        resources: this.api.resources,
        plans: this.UserService.isUserHasPermissions(['api-plan-r', 'api-plan-u']) && this.api.plans != null ? this.api.plans : [],
        properties: this.api.properties,
        'flow-mode': this.api.flow_mode,
      };
      this.studio.services = this.api.services;
      this.studio.removeAttribute('dirty');
    }
  }

  onChangeTab({ detail }) {
    this.$location.hash(detail);
    this.$rootScope.$digest();
  }

  onSelectFlows({ detail: { flows } }) {
    this.$location.search('flows', flows);
    this.$rootScope.$digest();
  }

  fetchPolicyDocumentation({ detail }) {
    const policy = detail.policy;
    this.PolicyService.getDocumentation(policy.id)
      .then((response) => {
        this.studio.documentation = { content: response.data, image: policy.icon, id: policy.id };
      })
      .catch(() => (this.studio.documentation = null));
  }

  fetchResourceDocumentation(event) {
    const {
      detail: { resourceType, target },
    } = event;
    this.ResourceService.getDocumentation(resourceType.id)
      .then((response) => {
        target.documentation = { content: response.data, image: resourceType.icon };
      })
      .catch(() => (target.documentation = null));
  }

  fetchSpelGrammar({ detail }) {
    this.SpelService.getGrammar().then((response) => {
      detail.currentTarget.grammar = response.data;
    });
  }

  onSave({ detail: { definition, services } }) {
    this.api.flows = definition.flows;
    this.api.plans = definition.plans;
    this.api.resources = definition.resources;
    this.api.properties = definition.properties;
    this.api.services = services;
    this.api.flow_mode = definition['flow-mode'];
    this.ApiService.update(this.api).then((updatedApi) => {
      this.NotificationService.show('Design of api has been updated');
      this.studio.saved();
      this.$rootScope.$broadcast('apiChangeSuccess', { api: this.api });
    });
  }
}

export default ApiPolicyStudioController;
