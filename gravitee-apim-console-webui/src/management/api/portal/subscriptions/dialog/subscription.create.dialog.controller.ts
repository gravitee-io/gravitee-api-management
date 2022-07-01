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
import * as _ from 'lodash';

import { ApiService } from '../../../../../services/api.service';
import ApplicationService from '../../../../../services/application.service';
import { PlanSecurityType } from '../../../../../entities/plan/plan';

function DialogSubscriptionCreateController(
  $mdDialog: angular.material.IDialogService,
  plans,
  api,
  ApplicationService: ApplicationService,
  ApiService: ApiService,
  Constants: any,
) {
  'ngInject';
  this.api = api;
  this.plans = plans;
  this.selectedApp = null;
  this.selectedPlan = null;
  this.selectedPlanCustomApiKey = null;
  this.plansWithSubscriptions = [];

  this.$onInit = () => {
    this.canUseCustomApiKey = Constants.env.settings.plan.security.customApiKey.enabled;
  };

  this.hide = function () {
    $mdDialog.cancel();
  };

  this.save = function () {
    if (this.selectedApp && this.selectedPlan) {
      $mdDialog.hide({
        application: this.selectedApp,
        plan: this.selectedPlan,
        customApiKey: this.selectedPlanCustomApiKey,
      });
    }
  };

  this.planAlreadyHaveSubscriptions = function (planId) {
    return _.indexOf(this.plansWithSubscriptions, planId) > -1;
  };

  this.planIsApiKey = (planId) => {
    return this.plans.find((p) => p.security === PlanSecurityType.API_KEY && p.id === planId) != null;
  };

  this.selectedItemChange = function () {
    this.plansWithSubscriptions = [];
    this.selectedPlanApiKey = null;
    if (this.selectedApp) {
      ApiService.getSubscriptions(this.api.id, '?application=' + this.selectedApp.id + '&status=pending,accepted').then((response) => {
        this.plansWithSubscriptions = _.map(response.data.data, (subscription) => {
          return subscription.plan;
        });
        if (this.selectedPlan && this.planAlreadyHaveSubscriptions(this.selectedPlan.id)) {
          this.selectedPlan = null;
        }
      });
    }
  };

  this.searchApplication = function (searchedAppName) {
    return ApplicationService.search(searchedAppName).then((response) => {
      return response.data;
    });
  };

  this.hasGeneralConditions = function (plan) {
    return plan.general_conditions !== undefined && plan.general_conditions !== null;
  };

  this.atLeastOnePlanWithGeneralConditions = function () {
    return this.plans.find((p) => p.general_conditions !== undefined && p.general_conditions !== '') != null;
  };

  this.onApiKeyValueChange = (apiKeyValidatedInput) => {
    this.selectedPlanCustomApiKey = apiKeyValidatedInput.customApiKey;
    this.customApiKeyInputState = apiKeyValidatedInput.customApiKeyInputState;
  };
}

export default DialogSubscriptionCreateController;
