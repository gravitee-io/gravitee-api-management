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
function DialogClosePlanController($scope, $rootScope, $mdDialog, ApiService, NotificationService, apiId, plan, subscriptions) {
  'ngInject';

  $scope.apiId = apiId;
  $scope.plan = plan;
	$scope.subscriptions = subscriptions;

  $scope.hide = function () {
     $mdDialog.cancel();
  };

  $scope.close = function () {
    if ($scope.plan.security === 'api_key' && $scope.subscriptions === 0) {
      ApiService.deletePlan($scope.apiId, $scope.plan.id).then(function() {
        NotificationService.show('Plan ' + plan.name + ' has been deleted');
      }).catch(function (error) {
        NotificationService.show('Error while deleting plan ' + plan.name);
        $scope.error = error;
      });
    } else {
      ApiService.closePlan($scope.apiId, $scope.plan.id).then(function() {
        NotificationService.show('Plan ' + plan.name + ' has been closed');
        $rootScope.$broadcast("planChangeSuccess", { state: "closed"});
      }).catch(function (error) {
        NotificationService.show('Error while closing plan ' + plan.name);
        $scope.error = error;
      });
    }

    $mdDialog.hide($scope.plan);
  };
}

export default DialogClosePlanController;
