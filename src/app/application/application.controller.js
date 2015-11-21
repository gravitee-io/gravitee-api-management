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
		this.applicationName = $stateParams.applicationName;
		this.application = {};
		this.applications = [];
		this.associatedAPIs = [];
		this.members = [];
		this.membershipTypes = [ 'OWNER', 'USER' ];
		if (this.applicationName) {
				this.get(this.applicationName);
				this.getAssociatedAPIs(this.applicationName);
				this.getMembers(this.applicationName);
		} else {
			this.list();
		}
	}

	get(name) {
		this.application = {
			"name": name,
			"description": name + "'s description",
			"type": "web"
		};
		//this.ApplicationService.get(name).then(response => {
    //  this.application = response.data;
    //});
  }

	getAssociatedAPIs(applicationName) {
		this.associatedAPIs = [
			{
				"api": {
					"name": "API 1",
					"version": "v1"
				},
				"apiKey": {
					"key": "16789-fGHJKLM-fgyhujik-123344",
					"revoked": false
				}
			},
			{
				"api": {
					"name": "API 2",
					"version": "v1"
				},
				"apiKey": {
					"key": "98766-DEDEDE-xcvbnbvn-765678",
					"revoked": false
				}
			}
		];
		//	this.ApplicationService.getAssociatedAPIs(applicationName).then(response => {
		//	var _associatedAPIs = response.data;
		//	var promises = [];
		//	for (var i = 0; i < _associatedAPIs.length; i++) {
		//		var api = _associatedAPIs[i];
		//		promises.push(this.ApplicationService.getAPIKey(applicationName, api.name));
		//	}
		//	this.$q.all(promises).then(results => {
		//		for (var i = 0; i < _associatedAPIs.length; i++) {
		//			var apiKey = { "api": _associatedAPIs[i], "apiKey" : results[i].data };
		//		  this.associatedAPIs.push(apiKey);
		//		}	
		//	});
		//});
	}

	getMembers(applicationName) {
		this.members = [ 
			{
				"user": "username1",
				"type" : "OWNER"
			},
			{
				"user": "username2",
				"type" : "USER"
			},
			{
				"user": "username3",
				"type" : "USER"
			}			
		];
		//this.ApplicationService.getMembers(applicationName).then(response => {
		//	this.members = response.data;
		//});
	}

	updateMember(member) {
		console.log(member);
		//this.AppplicationService.addOrUpdateMember(this.application.name, member).then(response => {
		//});
	}

	deleteMember(member) {
		var index = this.members.indexOf(member);
  	this.members.splice(index, 1);
		this.NotificationService.show("Member " + member.user + " removed");    
		//this.AppplicationService.deleteMember(this.application.name, member.user).then(response => {
		//	this.members.splice(index, 1);
		//	this.NotificationService.show("Member " + member.user + " has been removed successfully");		
		//});
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

	unsubscribeAPI(application, apiName, apiKey) {
		this.ApplicationService.unsubscribe(application, apiName, apiKey).then(response => {
			this.NotificationService.show('Application unsubscribed');
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
        that.list();
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
        that.list();
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

	$scope.apis = [];
	$scope.apisSelected = [];
	$scope.application = application;

	ApiService.list().then(function(response) {
		var _apis = response.data;
		for(var i = 0; i < _apis.length; i++) {
			var _api = _apis[i];
			var exist = false;
			for(var j = 0; j < associatedAPIs.length; j++) {
				if (_api.name === associatedAPIs[j].api.name) {
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

	$scope.selectApi = function(apiName) {
		var idx = $scope.apisSelected.indexOf(apiName);
    if (idx > -1) {
      $scope.apisSelected.splice(idx, 1);
    }
    else {
      $scope.apisSelected.push(apiName);
    }
	};

	$scope.subscribe = function(application) {
		for (var i = 0; i < $scope.apisSelected.length; i++) {
				var apiName = $scope.apisSelected[i];
				ApplicationService.subscribe(application, apiName).then(function() {
					NotificationService.show('Application has subscribed to api ' + apiName);
				}).catch(function (error) {
					NotificationService.show('Error while subscribing for api ' + apiName);
				  $scope.error = error;
				});
		}
	};
}

function DialogAddMemberController($scope, $mdDialog, application, ApplicationService, UserService, NotificationService) {
  'ngInject';

	$scope.user = {};

  $scope.hide = function () {
     $mdDialog.cancel();
  };

	$scope.searchUser = function (query) {
		UserService.findByName(query).then(function(response) {
			$scope.user = response.data;
		}).catch(function (error) {
			NotificationService.show('Error while searching members');
      $scope.error = error;
		});
	};

  $scope.addMembers = function (application, member) {
    ApplicationService.addMember(application.name, member).then(function () {
			NotificationService.show('Member added');
      $mdDialog.hide(application);
    }).catch(function (error) {
			NotificationService.show('Error while adding members');
      $scope.error = error;
    });
  };
}

export default ApplicationController;
