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
function DialogPublishPlanController($scope, $rootScope, $mdDialog, ApiService, NotificationService, apiId, plan) {
  'ngInject';

  $scope.apiId = apiId;
  $scope.plan = plan;

  $scope.hide = function () {
     $mdDialog.cancel();
  };

  $scope.publish = function () {
    ApiService.publishPlan($scope.apiId, $scope.plan.id).then(function() {
      NotificationService.show('Plan ' + plan.name + ' has been published');
      $rootScope.$broadcast("planChangeSuccess", { state: "published"});
    }).catch(function (error) {
      $scope.error = error;
    });

    $mdDialog.hide($scope.plan);
  };
}

export default DialogPublishPlanController;
