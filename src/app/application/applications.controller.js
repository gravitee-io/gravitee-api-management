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
  constructor($window, $mdDialog, $state, ApplicationService, NotificationService) {
		'ngInject';
    this.$window = $window;
		this.$mdDialog = $mdDialog;
		this.$state = $state;
		this.ApplicationService = ApplicationService;
		this.NotificationService = NotificationService;
		this.applications = [];
		this.list();
		this.tableMode = $state.current.name.endsWith('table')? true : false;
	}

	list() {
		this.ApplicationService.list().then(response => {
			this.applications = response.data;
		});
  }
	
	showAddApplicationModal(ev) {
    var _that = this;
    this.$mdDialog.show({
      controller: 'DialogApplicationController',
      templateUrl: 'app/application/dialog/application.dialog.html',
      parent: angular.element(document.body),
			targetEvent: ev,
      clickOutsideToClose: true
    }).then(function (application) {
      if (application) {
        _that.$window.location.href = '#/applications/' + application.data.id + '/general';
      }
    }, function() {
       // You cancelled the dialog
    });
  }

	bgColorByIndex(index) {
    switch (index % 6) {
      case 0 :
        return '#f39c12';
      case 1 :
        return '#29b6f6';
      case 2 :
        return '#26c6da';
      case 3 :
        return '#26a69a';
      case 4 :
        return '#259b24';
      case 5 :
        return '#26a69a';
      default :
        return 'black';
    }
  }

	changeMode(tableMode) {
    this.tableMode = tableMode;
    this.$state.go(this.tableMode ? 'applications.table' : 'applications.thumb');
  }
}

export default ApplicationsController;
