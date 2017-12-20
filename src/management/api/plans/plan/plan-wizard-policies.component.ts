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
import PolicyService from "../../../../services/policy.service";
import ApiEditPlanController from "./edit-plan.controller";

class Policy {
  id: string;
  title: string;
  description: string;
  enable?: boolean = false;
  schema?: string;
  model?: object;
  form?: ng.IFormController;
}

const ApiPlanWizardPoliciesComponent: ng.IComponentOptions = {
  require: {
    parent: '^editPlan'
  },
  template: require("./plan-wizard-policies.html"),
  controller: class {
    private policies: Policy[];
    private methods: string[] = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'];
    private parent: ApiEditPlanController;

    constructor(
      private PolicyService: PolicyService) {
      'ngInject';

      this.policies = [
        {
          id: 'rate-limit', title: 'Rate-Limiting',
          description: 'Rate limit how many HTTP requests an application can make in a given period of seconds or minutes'
        }, {
          id: 'quota', title: 'Quota',
          description: 'Rate limit how many HTTP requests an application can make in a given period of hours, days or months'
        }, {
          id: 'resource-filtering', title: 'Path Authorization',
          description: 'Restrict paths according to whitelist and / or blacklist rules'
        }
      ];
    }

    $onInit() {
      if (! this.parent.plan.paths) {
        this.parent.plan.paths = {'/': []};
      }

      _.each(this.policies, policy => {
        this.PolicyService.getSchema(policy.id).then(schema => {
          policy.schema = schema.data;

          let pathPolicy = _.find(this.parent.plan.paths['/'], function(pathPolicy) {
            return pathPolicy[policy.id] != undefined;
          });

          if (pathPolicy && pathPolicy[policy.id]) {
            policy.enable = true;
            policy.model = pathPolicy[policy.id];
          } else {
            policy.model = {};
          }
        });
      });
    }

    validate() {
      return ! _.find(this.policies, policy => {
        return policy.enable && policy.form && policy.form.$invalid;
      });
    }

    gotoNextStep() {
      let root = this.parent.plan.paths['/'];
      root.length = 0;

      _(this.policies)
        .filter(policy => policy.enable)
        .map(policy => {
          let p = {
            methods: this.methods,
            enable: true
          };
          p[policy.id] = policy.model;

          return p;
        })
        .each(policy => root.push(policy));

      this.parent.saveOrUpdate();
    };
  }
};





export default ApiPlanWizardPoliciesComponent;
