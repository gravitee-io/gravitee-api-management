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
class ApplicationController {
  constructor($stateParams, $mdDialog, ApplicationService) {
		'ngInject';
		this.$stateParams = $stateParams;
		this.$mdDialog = $mdDialog;
		this.ApplicationService = ApplicationService;
		this.applicationName = $stateParams.applicationName;
		this.application = {};
		this.applications = [];
		
		if (this.applicationName) {
				this.get(this.applicationName); 
		} else {
			this.list();
		}
	}

	get(name) {
		this.ApplicationService.get(name).then(response => {
      this.application = response.data;
    });
  }
	
	list() {
		this.ApplicationService.list().then(response => {
			this.applications = response.data;		
		});
  }

  update(application) {
		this.ApplicationService.update(application).then(response => {
			this.list();
		});
  }

  delete(name) {
		this.ApplicationService.delete(name).then(response => {
			this.list();
		});
  }

	showAddApplicationModal(ev) {
    var that = this;
    this.$mdDialog.show({
      controller: DialogApplicationController,
      templateUrl: 'app/application/application.dialog.html',
      parent: angular.element(document.body),
			targetEvent: ev,
      clickOutsideToClose: true
    }).then(function (application) {
      if (application) {
        that.list();
      }
    }, function() {
       // You cancelled the dialog
    });
  }
}

function DialogApplicationController($scope, $mdDialog, ApplicationService) {
  'ngInject';

  $scope.hide = function () {
     $mdDialog.cancel();
  };

  $scope.create = function (application) {
    ApplicationService.create(application).then(function () {
      $mdDialog.hide(application);
    }).catch(function (error) {
      $scope.error = error;
    });
  };
}

export default ApplicationController;
