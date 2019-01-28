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
import PolicyService from "../../../../../services/policy.service";
import ApiEditPlanController from "./edit-plan.controller";

const ApiPlanWizardPoliciesComponent: ng.IComponentOptions = {
  require: {
    parent: '^editPlan'
  },
  template: require("./plan-wizard-policies.html"),
  controller: class {
    private parent: ApiEditPlanController;
    private selectedPolicy: any;
    private editablePolicy: any;

    private policyDefinition: any;
    private policySchema: any;

    constructor(
      private $mdDialog: angular.material.IDialogService,
      private PolicyService: PolicyService) {
      'ngInject';

    }

    addPolicy() {
      this.editablePolicy = null;
      let policySchema = this.parent.policies.find( policy => policy.id === this.selectedPolicy);

      // Configure the policy with default values
      let policy = {
        id: policySchema.id,
        name: policySchema.name,
        enabled: true,
        description: policySchema.description
      };

      let idx = this.parent.planPolicies.push(policy);

      // Restore selected policy to empty
      this.selectedPolicy = null;
      this.editPolicy(idx-1);
    }

    getPolicyClass(policy) {
      const classes = [];
      const selected = this.editablePolicy && this.editablePolicy.$$hashKey === policy.$$hashKey;
      if (selected) {
        classes.push("gravitee-policy-card-selected");
      }

      if (!selected && ! policy.enabled) {
        classes.push("gravitee-policy-card-disabled");
      }

      if (!policy.name) {
        classes.push("gravitee-policy-card-missed");
      }
      return classes.join(' ');
    }

    editPolicy(index, ev?:any) {
      if (ev) {
        ev.stopPropagation();
      }

      // Manage unselect
      if (this.editablePolicy === this.parent.planPolicies[index]) {
        this.editablePolicy = null;
      } else {
        this.editablePolicy = this.parent.planPolicies[index];

        let model = this.editablePolicy && this.editablePolicy[this.editablePolicy.id];

        this.policyDefinition = (model) ? model : {};
        this.editablePolicy[this.editablePolicy.id] = this.policyDefinition;

        this.PolicyService.getSchema(this.editablePolicy.id).then(schema => {
          this.policySchema = schema.data;

          if (!this.policySchema || Object.keys(this.policySchema).length === 0) {
            this.policySchema = {
              "type": "object",
              "id": "empty",
              "properties": {"": {}}
            };
          }
        });
      }
    }

    editPolicyDescription(index, path, ev) {
      ev.stopPropagation();
      this.editablePolicy = null;

      const policy = this.parent.planPolicies[index];

      this.$mdDialog.show({
        controller: 'DialogEditPolicyController',
        controllerAs: 'editPolicyDialogCtrl',
        template: require('../../../design/policies/dialog/policy.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          description: policy.description
        }
      }).then(description => policy.description = description, () => {});
    }

    removePolicy(index, path, ev) {
      ev.stopPropagation();
      let that = this;
      this.$mdDialog.show({
        controller: 'DialogConfirmController',
        controllerAs: 'ctrl',
        template: require('../../../../../components/dialog/confirmWarning.dialog.html'),
        clickOutsideToClose: true,
        locals: {
          title: 'Are you sure you want to remove this policy ?',
          confirmButton: 'Remove'
        }
      }).then(function (response) {
        if (response) {
          that.editablePolicy = null;
          that.parent.planPolicies.splice(index, 1);
        }
      });
    }

    switchPolicyEnabled(index, path, ev) {
      ev.stopPropagation();
      this.editablePolicy = null;

      const policy = this.parent.planPolicies[index];
      policy.enabled = !policy.enabled;
    }

    gotoNextStep() {
      // Clean policy definition from plan policies
      let policies = JSON.parse(JSON.stringify(this.parent.planPolicies));
      policies.forEach(policy => {
        delete policy.$$hashKey;
        delete policy.id;
        delete policy.name;
      });

      this.parent.plan.paths['/'] = this.parent.restrictionsPolicies.concat(policies);
      this.parent.saveOrUpdate();
    };
  }
};

export default ApiPlanWizardPoliciesComponent;
