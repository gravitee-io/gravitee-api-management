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
  constructor($stateParams, $mdDialog, $q, ApplicationService, NotificationService) {
		'ngInject';
		this.$stateParams = $stateParams;
		this.$mdDialog = $mdDialog;
		this.$q = $q;
		this.ApplicationService = ApplicationService;
		this.NotificationService = NotificationService;
		this.applicationId = $stateParams.applicationId;
		this.application = {};
		this.applications = [];
		this.associatedAPIs = [];
		this.members = [];
		this.membershipTypes = [ 'PRIMARY_OWNER', 'OWNER', 'USER' ];
		if (this.applicationId) {
				this.get(this.applicationId);
				this.getAssociatedAPIs(this.applicationId);
				this.getMembers(this.applicationId);
		} else {
			this.list();
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

	list() {
		this.ApplicationService.list().then(response => {
			this.applications = response.data;		
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

	showSubscribeApiModal(ev) {
		var that = this;
    this.$mdDialog.show({
      controller: DialogSubscribeApiController,
      templateUrl: 'app/application/subscribeApi.dialog.html',
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
      controller: DialogAddMemberController,
      templateUrl: 'app/application/addMember.dialog.html',
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

function DialogApplicationController($scope, $mdDialog, ApplicationService, NotificationService) {
  'ngInject';

  $scope.hide = function () {
     $mdDialog.cancel();
  };

  $scope.create = function (application) {
    ApplicationService.create(application).then(function () {
			NotificationService.show('Application created');
      $mdDialog.hide(application);
    }).catch(function (error) {
			NotificationService.show('Error while creating the application');
      $scope.error = error;
    });
  };
}

function DialogSubscribeApiController($scope, $mdDialog, application, associatedAPIs, ApplicationService, NotificationService, ApiService) {
  'ngInject';

	$scope.searchAPI = "";
	$scope.apis = [];
	$scope.apisSelected = [];
	$scope.application = application;

	ApiService.list().then(function(response) {
		var _apis = response.data;
		for(var i = 0; i < _apis.length; i++) {
			var _api = _apis[i];
			var exist = false;
			for(var j = 0; j < associatedAPIs.length; j++) {
				if (_api.id === associatedAPIs[j].api.id) {
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

	$scope.selectApi = function(api) {
		var idx = $scope.apisSelected.indexOf(api.id);
    if (idx > -1) {
      $scope.apisSelected.splice(idx, 1);
    }
    else {
      $scope.apisSelected.push(api.id);
    }
	};

	$scope.subscribe = function(application) {
		for (var i = 0; i < $scope.apisSelected.length; i++) {
				var apiId = $scope.apisSelected[i];
				ApplicationService.subscribe(application, apiId).then(function() {
					NotificationService.show('Application has subscribed to api ' + apiId);
				}).catch(function (error) {
					NotificationService.show('Error while subscribing for api ' + apiId);
				  $scope.error = error;
				});
		}
		$mdDialog.hide(application);
	};
}

function DialogAddMemberController($scope, $mdDialog, application, ApplicationService, UserService, NotificationService) {
  'ngInject';

	$scope.application = application;
	$scope.user = {};
	$scope.usersFound = [];
	$scope.usersSelected = [];
	$scope.searchText = "";
	
  $scope.hide = function () {
     $mdDialog.cancel();
  };

	$scope.searchUser = function (query) {
		if (query) {
			return UserService.findLDAP(query).then(function(response) {
				return response.data;
			});
		}
	};

  $scope.selectedItemChange = function(item) {
		if (item) {
			$scope.usersFound.push(item);
			$scope.searchText = "";
		}
  }

	$scope.selectMember = function(user) {
		var idx = $scope.usersSelected.indexOf(user.username);
    if (idx > -1) {
      $scope.usersSelected.splice(idx, 1);
    }
    else {
      $scope.usersSelected.push(user.username);
    }
	};

  $scope.addMembers = function () {
		for (var i = 0; i < $scope.usersSelected.length; i++) {
			var username = $scope.usersSelected[i];
			var member = {
				"user" : username,
				"type" : "USER"
			};
			ApplicationService.addOrUpdateMember($scope.application.id, member).then(function() {
				NotificationService.show('Member ' + username + ' added');
			}).catch(function (error) {
				NotificationService.show('Error while adding member ' + username);
			  $scope.error = error;
			});
		}
		$mdDialog.hide($scope.application);
  };
}

export default ApplicationController;
