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
import FlowService from '../../services/flow.service';
import PolicyService from '../../services/policy.service';
import ResourceService from '../../services/resource.service';
import SpelService from '../../services/spel.service';
import '@gravitee/ui-components/wc/gv-policy-studio';

const OldPolicyStudioWrapperComponent: ng.IComponentOptions = {
  bindings: {
    flowsTitle: '@',
    canAdd: '@',
    canDebug: '@',
    hasPolicyFilter: '@',
    hasProperties: '@',
    hasResources: '@',
    sortable: '@',
    hasPlans: '@',
    definition: '<',
    services: '<',
    readonlyPlans: '<',
    propertyProviders: '<',
    resourceTypes: '<',
    flowSchema: '<',
    withoutResource: '@',
    policies: '<',
    dynamicPropertySchema: '<',
    debugResponse: '<',
    hasConditionalSteps: '@',
  },
  template: require('./old-policy-studio-wrapper.html'),
  controller: function (
    PolicyService: PolicyService,
    FlowService: FlowService,
    ResourceService: ResourceService,
    SpelService: SpelService,
    $location: ng.ILocationService,
  ) {
    'ngInject';

    const CATEGORY_POLICY = ['security', 'performance', 'transformation', 'others'];

    this.configurationInformation =
      'By default, the selection of a flow is based on the operator defined in the flow itself. This operator allows either to select a flow when the path matches exactly, or when the start of the path matches. The "Best match" option allows you to select the flow from the path that is closest.';

    this.$onInit = async () => {
      this.component = document.querySelector('gv-policy-studio');
      this.policies = sortByCategory(this.policies);

      FlowService.getConfigurationSchema().then(({ data }) => {
        this.configurationSchema = data;
      });

      this.component.setAttribute('tab-id', $location.hash());
      let selectedFlows = null;
      const flowsParam = $location.search().flows;
      if (typeof flowsParam === 'string') {
        selectedFlows = [flowsParam];
      } else if (Array.isArray(flowsParam)) {
        selectedFlows = flowsParam;
      }
      this.component.setAttribute('selected-flows-id', JSON.stringify(selectedFlows));
    };

    this.onChangeTab = ({ detail }) => {
      $location.hash(detail);
    };

    this.onSelectFlows = ({ detail: { flows } }) => {
      $location.search('flows', flows);
    };

    this.fetchPolicyDocumentation = ({ detail }) => {
      const policy = detail.policy;
      PolicyService.getDocumentation(policy.id)
        .then((response) => {
          this.component.documentation = { content: response.data, image: policy.icon, id: policy.id };
        })
        .catch(() => (this.studio.documentation = null));
    };

    this.fetchResourceDocumentation = (event) => {
      const {
        detail: { resourceType, target },
      } = event;
      ResourceService.getDocumentation(resourceType.id)
        .then((response) => {
          target.documentation = { content: response.data, image: resourceType.icon };
        })
        .catch(() => (target.documentation = null));
    };

    this.fetchSpelGrammar = ({ detail }) => {
      SpelService.getGrammar().then((response) => {
        detail.currentTarget.grammar = response.data;
      });
    };

    function sortByCategory(policies: any) {
      return policies.sort((a, b) => {
        if (a.category == null) {
          a.category = CATEGORY_POLICY[3];
        }
        if (b.category == null) {
          b.category = CATEGORY_POLICY[3];
        }
        if (a.category === b.category) {
          return 0;
        }
        const aKind = CATEGORY_POLICY.indexOf(a.category);
        if (aKind === -1) {
          return 1;
        }
        const bKind = CATEGORY_POLICY.indexOf(b.category);
        if (bKind === -1) {
          return -1;
        }
        return aKind < bKind ? -1 : 1;
      });
    }
  },
};

export default OldPolicyStudioWrapperComponent;
