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

const ApiPlanWizardSecurityComponent: ng.IComponentOptions = {
  require: {
    parent: '^editPlan'
  },
  template: require('./plan-wizard-security.html'),
  controller: class {
    private securityTypes: any[];
    private securityDefinition: any;
    private securitySchema: any;
    private parent: ApiEditPlanController;

    constructor(private PolicyService: PolicyService, Constants: any) {
      'ngInject';

      this.securityTypes = _.filter([
        {
          'id': 'oauth2',
          'name': 'OAuth2',
          'policy': 'oauth2'
        }, {
          'id': 'jwt',
          'name': 'JWT',
          'policy': 'jwt'
        }, {
          'id': 'api_key',
          'name': 'API Key',
          'policy': 'api-key'
        }, {
          'id': 'key_less',
          'name': 'Keyless (public)'
        }], (security) => {
          return Constants.plan.security[_.replace(security.id, '_', '')].enabled;
      });
    }

    $onInit() {
        if (this.parent.plan.security) {
          this.onSecurityTypeChange();
        }
    }

    onSecurityTypeChange() {
      let securityType: any = _.find(this.securityTypes, {'id': this.parent.plan.security});
      if (securityType && securityType.policy) {
        this.PolicyService.getSchema(securityType.policy).then(schema => {
          this.securitySchema = schema.data;

          if (this.parent.plan.securityDefinition) {
            try {
              this.parent.plan.securityDefinition = JSON.parse(this.parent.plan.securityDefinition);

              // Try a double parsing (it appears that sometimes the json of security definition is double-encoded
              this.parent.plan.securityDefinition = JSON.parse(this.parent.plan.securityDefinition);
            } catch (e) {

            }
          } else {
            this.parent.plan.securityDefinition = {};
          }
        });
      } else {
        this.securitySchema = undefined;
        this.parent.plan.securityDefinition = {};
      }

      if (this.parent.plan.id === undefined) {
        this.parent.plan.securityDefinition = {};
      }
    }

    gotoNextStep() {
      this.parent.vm.stepData[1].data = this.parent.plan;
      this.parent.moveToNextStep(this.parent.vm.stepData[1]);
    }
  }
};

export default ApiPlanWizardSecurityComponent;
