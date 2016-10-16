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
function DialogSubscribeApiController($scope, $mdDialog, application, subscriptions, ApplicationService, NotificationService, ApiService) {
  'ngInject';

	$scope.searchAPI = "";
	$scope.apis = [];
	$scope.apisSelected = [];
	$scope.apisFound = [];
	$scope.application = application;

	ApiService.list().then(function(response) {
		var _apis = response.data;
		for(var i = 0; i < _apis.length; i++) {
			var _api = _apis[i];
			var exist = false;
			for (var key in subscriptions) {
				if (_api.id === key) {
					exist = true;
					break;
				}
			}
			if (!exist) {
				$scope.apis.push(_api);
			}
		}
	});

  $scope.hide = function () {
     $mdDialog.cancel();
  };

	$scope.selectedItemChange = function(item) {
		if (item) {
			if (!$scope.isAPISelected(item)) {
				$scope.apisFound.push(item);
				$scope.selectAPI(item);
			}
		}
  };

	$scope.selectAPI = function(api) {
		var idx = $scope.apisSelected.indexOf(api.id);
    if (idx > -1) {
      $scope.apisSelected.splice(idx, 1);
    }
    else {
      $scope.apisSelected.push(api.id);
    }
	};

	$scope.isAPISelected = function(api) {
		var idx = $scope.apisSelected.indexOf(api.id);
    return idx > -1;
	};

	$scope.subscribe = function(application) {
		for (var i = 0; i < $scope.apisSelected.length; i++) {
				var apiId = $scope.apisSelected[i];
				ApplicationService.subscribe(application, apiId).then(function() {
					NotificationService.show('Application has subscribed to api ' + apiId);
				});
		}
		$mdDialog.hide(application);
	};
}

export default DialogSubscribeApiController;
