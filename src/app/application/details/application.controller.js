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
  constructor($stateParams, $mdDialog, $q, $state, $scope, ApplicationService, NotificationService) {
		'ngInject';
		this.$stateParams = $stateParams;
		this.$mdDialog = $mdDialog;
		this.$q = $q;
		this.$state = $state;
		this.ApplicationService = ApplicationService;
		this.NotificationService = NotificationService;
		this.applicationId = $stateParams.applicationId;
		this.application = {};
		this.applications = [];
		this.associatedAPIs = [];
		this.members = [];
		this.membershipTypes = [ 'primary_owner', 'owner', 'user' ];
		if (this.applicationId) {
				this.get(this.applicationId);
				this.getAssociatedAPIs(this.applicationId);
				this.getMembers(this.applicationId);
		}
		if ($state.current.name.endsWith('dashboard')) {
      $scope.selectedTab = 0;
    } else if ($state.current.name.endsWith('general')) {
      $scope.selectedTab = 1;
    } else if ($state.current.name.endsWith('apis')) {
      $scope.selectedTab = 2;
    } else if ($state.current.name.endsWith('members')) {
      $scope.selectedTab = 3;
    }
	}

	get(applicationId) {
		this.ApplicationService.get(applicationId).then(response => {
      this.application = response.data;
    });
  }

	getAssociatedAPIs(applicationId) {
		this.ApplicationService.getAssociatedAPIs(applicationId).then(response => {
			var _associatedAPIs = response.data;
			var promises = [];
			for (var i = 0; i < _associatedAPIs.length; i++) {
				var api = _associatedAPIs[i];
				promises.push(this.ApplicationService.getAPIKey(applicationId, api.id));
			}
			this.$q.all(promises).then(results => {
				for (var i = 0; i < _associatedAPIs.length; i++) {
					var apiKey = { "api": _associatedAPIs[i], "apiKey" : results[i].data };
					this.associatedAPIs.push(apiKey);
				}	
			});
		});
	}

	getMembers(applicationId) {
		this.ApplicationService.getMembers(applicationId).then(response => {
			this.members = response.data;
		});
	}

	updateMember(member) {
		console.log(JSON.stringify(member));
		this.ApplicationService.addOrUpdateMember(this.application.id, member).then(response => {
			this.NotificationService.show('Member updated');
		});
	}

	deleteMember(member) {
		var index = this.members.indexOf(member);
		this.ApplicationService.deleteMember(this.application.id, member.user).then(response => {
			this.members.splice(index, 1);
			this.NotificationService.show("Member " + member.user + " has been removed successfully");		
		});
	}

  update(application) {
		this.ApplicationService.update(application).then(response => {
			this.NotificationService.show('Application updated');
		});
  }

  delete(name) {
		this.ApplicationService.delete(name).then(response => {
			this.list();
		});
  }

	unsubscribeAPI(application, apiId, apiKey) {
		this.ApplicationService.unsubscribe(application, apiId, apiKey).then(response => {
			this.NotificationService.show('Application unsubscribed');
		});
	}

	generateAPIKey(application, apiId) {
		this.ApplicationService.subscribe(application, apiId).then(response => {
			this.NotificationService.show('New API Key created');
			this.getAssociatedAPIs(application);
		});
	}

	showSubscribeApiModal(ev) {
		var that = this;
    this.$mdDialog.show({
      controller: 'DialogSubscribeApiController',
      templateUrl: 'app/application/dialog/subscribeApi.dialog.html',
      parent: angular.element(document.body),
			targetEvent: ev,
      clickOutsideToClose: true,
			application: that.application,
			associatedAPIs: that.associatedAPIs
    }).then(function (application) {
      if (application) {
        that.getAssociatedAPIs(application.id);
      }
    }, function() {
       // You cancelled the dialog
    });
	}

	showAddMemberModal(ev) {
		var that = this;
    this.$mdDialog.show({
      controller: 'DialogAddMemberController',
      templateUrl: 'app/application/dialog/addMember.dialog.html',
      parent: angular.element(document.body),
			targetEvent: ev,
      clickOutsideToClose: true,
			application: that.application
    }).then(function (application) {
      if (application) {
        that.getMembers(application.id);
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
}

export default ApplicationController;
