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
import _ = require('lodash');
import PolicyService from '../../../../../services/policy.service';
import ApiEditPlanController from './edit-plan.controller';

class Policy {
  id: string;
  title: string;
  description: string;
  enabled?: boolean = false;
  schema?: string;
  model?: object;
  form?: ng.IFormController;
}

const ApiPlanWizardRestrictionsComponent: ng.IComponentOptions = {
  require: {
    parent: '^editPlan',
  },
  template: require('./plan-wizard-restrictions.html'),
  controller: class {
    private policies: Policy[];
    private methods: string[] = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];
    private parent: ApiEditPlanController;

    constructor(private PolicyService: PolicyService) {
      'ngInject';

      this.policies = [
        {
          id: 'rate-limit',
          title: 'Rate Limiting',
          description: 'Rate limit how many HTTP requests an application can make in a given period of seconds or minutes',
        },
        {
          id: 'quota',
          title: 'Quota',
          description: 'Rate limit how many HTTP requests an application can make in a given period of hours, days or months',
        },
        {
          id: 'resource-filtering',
          title: 'Resource Filtering',
          description: 'Restrict resources according to whitelist and / or blacklist rules',
        },
      ];
    }

    $onInit() {
      // Extract plan "restriction" policies
      _.each(this.policies, (policy) => {
        this.PolicyService.getSchema(policy.id).then((schema) => {
          policy.schema = schema.data;
          if (!this.parent.isV2()) {
            let idx = this.parent.planPolicies.findIndex((pathPolicy) => pathPolicy[policy.id] != null);
            if (idx !== -1) {
              let restrictionPolicy = this.parent.planPolicies.splice(idx, 1)[0];
              policy.enabled = true;
              policy.model = restrictionPolicy[policy.id];
            } else {
              policy.model = {};
            }
          } else {
            policy.model = {};
          }
        });
      });
    }

    validate() {
      return !_.find(this.policies, (policy) => {
        return policy.enabled && policy.form && policy.form.$invalid;
      });
    }

    gotoNextStep() {
      this.parent.restrictionsPolicies = [];

      _(this.policies)
        .filter((policy) => policy.enabled)
        .map((policy) => {
          let restrictPolicy = {
            methods: this.methods,
            enabled: true,
          };
          restrictPolicy[policy.id] = policy.model;

          return restrictPolicy;
        })
        .each((policy) => this.parent.restrictionsPolicies.push(policy));

      this.parent.vm.stepData[2].data = this.parent.plan;
      if (!this.parent.hasPoliciesStep()) {
        const me = this;
        const pre = this.parent.restrictionsPolicies.map((restriction) => {
          const { methods, enabled, ...rest } = restriction;
          const policyId = Object.keys(rest)[0];
          const policy = me.policies.find((policy) => policy.id === policyId);
          const name = policy.title;
          const description = policy.description;
          const configuration = restriction[policyId];
          return {
            name,
            description,
            enabled,
            configuration,
            policy: policyId,
          };
        });
        this.parent.plan.flows = [
          {
            'path-operator': {
              path: '/',
              operator: 'STARTS_WITH',
            },
            enabled: true,
            pre,
            post: [],
          },
        ];
        delete this.parent.plan.paths;
        this.parent.saveOrUpdate();
      } else {
        this.parent.moveToNextStep(this.parent.vm.stepData[2]);
      }
    }
  },
};

export default ApiPlanWizardRestrictionsComponent;
