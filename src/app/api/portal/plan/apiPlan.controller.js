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
class ApiPortalPlanController {
  constructor (resolvedPlans, $mdDialog, NotificationService, $state, $scope) {
    'ngInject';
    this.plans = resolvedPlans.data;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$state = $state;

    $scope.subscribe = function(plan) {
      $mdDialog.show({
        controller: 'DialogSubscribePlanController',
        templateUrl: 'app/api/portal/plan/subscribePlan.dialog.html',
        clickOutsideToClose: true,
        plan: plan,
        resolve: {
          resolvedApplications: function (ApplicationService) {
            return ApplicationService.list();
          },
          resolvedSubscriptions: function (SubscriptionService) {
            return SubscriptionService.list(plan.id);
          }
        }
      }).then(function (application) {
        if (application) {
          $state.go('applications.portal.subscriptions', {applicationId: application.id});
        }
      });
    }
  }
}

export default ApiPortalPlanController;
