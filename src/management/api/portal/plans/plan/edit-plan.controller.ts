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
import ApiService from '../../../../../services/api.service';
import NotificationService from '../../../../../services/notification.service';
import {StateService} from '@uirouter/core';

class ApiEditPlanController {

  plan: any;
  groups: any[];
  api: any;
  policies: any[];
  tags: any[];
  userTags: any[];

  restrictionsPolicies: any[];
  planPolicies: any[];

  vm: {
    selectedStep: number;
    stepProgress: number;
    maxStep: number;
    showBusyText: boolean;
    stepData: {
      step: number;
      label?: string;
      completed: boolean;
      optional: boolean;
      data: any
    }[]
  };

  invalidPolicies = ['api-key', 'oauth2', 'jwt', 'rate-limit', 'quota', 'resource-filtering', 'xslt', 'xml-json',
    'rest-to-json', 'json-to-json', 'html-json', 'policy-assign-content'];

  constructor(
    private $scope,
    private $state: StateService,
    private $stateParams,
    private $timeout: ng.ITimeoutService,
    private ApiService: ApiService,
    private NotificationService: NotificationService
  ) {
    'ngInject';

    this.vm = {
      selectedStep: 0,
      stepProgress: 1,
      maxStep: 4,
      showBusyText: false,
      stepData: [
        {step: 1, completed: false, optional: false, data: {}},
        {step: 2, completed: false, optional: false, data: {}},
        {step: 3, completed: false, optional: true, data: {}},
        {step: 4, completed: false, optional: true, data: {}}
      ]
    };
  }

  $onInit() {
    if (!this.plan) {
      this.plan = {characteristics: []};
    }

    if (!this.plan.paths || !this.plan.paths['/']) {
      this.plan.paths = {'/': []};
    }

    this.planPolicies = this.plan.paths['/'];

    // Remove some policies from the "plan" policies scope
    _.remove(this.policies, (policy) => this.invalidPolicies.indexOf(policy.id) !== -1);

    // Add policy metadata
    if (this.planPolicies) {
      this.planPolicies.forEach(policy => {
        _.forEach(policy, (value, property) => {
          if (property !== "methods" && property !== "enabled" && property !== "description" && property !== "$$hashKey") {
            policy.id = property;
            let policyDef = this.policies.find(policyDef => policyDef.id === policy.id);
            if (policyDef) {
              policy.name = policyDef.name;
            }
          }
        });
      });
    }

    if (this.api.visibility === 'private') {
      if (this.api.groups) {
        const apiGroupIds = this.api.groups;
        this.groups = _.filter(this.groups, (group) => {
          return apiGroupIds.indexOf(group['id']) > -1;
        });
      } else {
        this.groups = [];
      }
    }

    if (this.plan['excluded_groups']) {
      this.plan.authorizedGroups = _.difference(_.map(this.groups, 'id'), this.plan['excluded_groups']);
    } else {
      this.plan.authorizedGroups = _.map(this.groups, 'id');
    }
  }

  moveToNextStep(step: any) {
    this.submitCurrentStep(step);
  }

  moveToPreviousStep() {
    if (this.vm.selectedStep > 0) {
      this.vm.selectedStep = this.vm.selectedStep - 1;
    }
  }

  selectStep(step) {
    this.vm.selectedStep = step;
  }

   submitCurrentStep(stepData) {
    this.vm.showBusyText = true;
    if (!stepData.completed) {
      if (this.vm.selectedStep !== 4) {
        this.vm.showBusyText = false;
        //move to next step when success
        stepData.completed = true;
        this.enableNextStep();
      }
    } else {
      this.vm.showBusyText = false;
      this.enableNextStep();
    }
  }

  enableNextStep() {
    //do not exceed into max step
    if (this.vm.selectedStep >= this.vm.maxStep) {
      return;
    }
    //do not increment vm.stepProgress when submitting from previously completed step
    if (this.vm.selectedStep === this.vm.stepProgress - 1) {
      this.vm.stepProgress = this.vm.stepProgress + 1;
    }

    this.$timeout(() => this.vm.selectedStep = this.vm.selectedStep + 1);
  }

  saveOrUpdate() {
    // Transform security definition to json
    this.plan.securityDefinition = JSON.stringify(this.plan.securityDefinition);

    // Convert authorized groups to excludedGroups
    this.plan.excludedGroups = [];
    if (this.groups) {
      this.plan.excludedGroups = _.difference(_.map(this.groups, 'id'), this.plan.authorizedGroups);
    }

    this.ApiService.savePlan(this.$stateParams.apiId, this.plan).then( () => {
      this.NotificationService.show(this.plan.name + ' has been saved successfully');
      this.$state.go(
        'management.apis.detail.portal.plans.list',
        this.plan.id === undefined ? {'state': 'staging'} : {});
      this.$scope.$parent.apiCtrl.checkAPISynchronization({id: this.$stateParams.apiId});
    });
  }

  isTagDisabled(tag: any): boolean {
    return !_.includes(this.userTags, tag.id) || !_.includes(this.api.tags, tag.id);
  }
}

export default ApiEditPlanController;
