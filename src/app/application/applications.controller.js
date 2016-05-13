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
class ApplicationsController {
  constructor($window, $mdDialog, $state, $rootScope, ApplicationService, NotificationService, resolvedApplications) {
		'ngInject';
    this.$window = $window;
		this.$mdDialog = $mdDialog;
		this.$state = $state;
		this.$rootScope = $rootScope;
		this.ApplicationService = ApplicationService;
		this.NotificationService = NotificationService;
		this.applications = resolvedApplications.data;
		this.tableMode = $state.current.name.endsWith('table')? true : false;
	}

	createInitApplication() {
		if (!this.$rootScope.graviteeUser) {
			this.$rootScope.$broadcast("authenticationRequired");
		} else {
			this.showAddApplicationModal();
		}
	}

	showAddApplicationModal(ev) {
    var _that = this;
    this.$mdDialog.show({
      controller: 'DialogApplicationController',
      templateUrl: 'app/application/dialog/application.dialog.html',
      parent: angular.element(document.body),
			targetEvent: ev
    }).then(function (application) {
      if (application) {
        _that.$window.location.href = '#/applications/' + application.data.id + '/general';
      }
    }, function() {
       // You cancelled the dialog
    });
  }

	changeMode(tableMode) {
    this.tableMode = tableMode;
    this.$state.go(this.tableMode ? 'applications.list.table' : 'applications.list.thumb');
  }
}

export default ApplicationsController;
