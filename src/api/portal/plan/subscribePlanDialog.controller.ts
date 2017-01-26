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

function DialogSubscribePlanController(
  $scope,
  $state,
  $mdDialog,
  plan,
  resolvedApplications,
  NotificationService,
  ApplicationService,
  resolvedSubscriptions
) {
  'ngInject';
  $scope.plan = plan;
  $scope.applications = resolvedApplications.data;
  $scope.subscribtionsByApplication = _.groupBy(resolvedSubscriptions.data, 'application.id');

  $scope.isNotSelectable = function (applicationId) {
    return _.find($scope.subscribtionsByApplication[applicationId], function (subscribtionByApplication: any) {
      return subscribtionByApplication.status === 'accepted' || subscribtionByApplication.status === 'pending';
    });
  };

  $scope.select = function (application) {
    ApplicationService.subscribe(application.id, plan.id).then(function () {
      $mdDialog.hide(application);
      NotificationService.show('Application has subscribed to plan ' + plan.name);
    });
  };

  $scope.goToApplications = function() {
    $mdDialog.cancel();
    $state.go('applications.list', {}, {reload: true});
  };

  $scope.hide = function () {
    $mdDialog.cancel();
  };
}

export default DialogSubscribePlanController;
