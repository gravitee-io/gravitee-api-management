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

function DialogSubscriptionCreateController($scope, $mdDialog, plans, ApplicationService) {
  'ngInject';
  this.plans = plans;
  this.selectedApp = null;
  this.selectedPlan = null;
  this.plansWithSubscriptions = [];


  this.hide = function () {
    $mdDialog.cancel();
  };

  this.save = function () {
    if (this.selectedApp && this.selectedPlan) {
      $mdDialog.hide({
          applicationId: this.selectedApp.id,
          planId: this.selectedPlan
      });
    }
  };

  this.planAlreadyHaveSubscriptions = function(planId) {
    return _.indexOf(this.plansWithSubscriptions, planId) > -1;
  };

  this.selectedItemChange = function () {
    this.plansWithSubscriptions = [];
    this.selectedPlan = null;
    if (this.selectedApp) {
      _.map(this.plans, (plan: {id: number}) => {
        ApplicationService.listSubscriptions(this.selectedApp.id, plan.id).then((response) => {
          var subs = _.filter(response.data, (sub: {status: string}) => {
            return sub.status === "pending" || sub.status === "accepted";
          });
          if (subs.length > 0) {
            this.plansWithSubscriptions.push(plan.id);
          }
        });
      });
    }
  };

  this.searchApplication = function(searchedAppName) {
      return ApplicationService.search(searchedAppName).then((response) => {
        return response.data;
      });
  };

}

export default DialogSubscriptionCreateController;
