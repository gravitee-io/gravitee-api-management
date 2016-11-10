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
class ApplicationGeneralController {
  constructor(resolvedApplication, ApplicationService, NotificationService, GroupService, UserService, $state, $scope, $mdDialog) {
    'ngInject';
    this.application = resolvedApplication.data;

    if (!this.application.group) {
      this.application.group = GroupService.getEmptyGroup();
    }
    this.groups = [this.application.group];
    this.initialApplication = _.cloneDeep(this.application);
    this.ApplicationService = ApplicationService;
    this.NotificationService = NotificationService;
    this.GroupService = GroupService;
    this.UserService = UserService;
    this.$scope = $scope;
    this.$state = $state;
    this.$mdDialog = $mdDialog;
  }

  get(applicationId) {
    this.ApplicationService.get(applicationId).then(response => {
      this.application = response.data;
      this.initialApplication = _.cloneDeep(this.application);
    });
  }

  update(application) {
    this.ApplicationService.update(application).then(() => {
      this.initialApplication = _.cloneDeep(application);
      this.$scope.formApplication.$setPristine();
      this.NotificationService.show('Application ' + application.name + ' has been updated');
    });
  }

  delete(application) {
    this.ApplicationService.delete(application).then(() => {
      this.NotificationService.show('Application ' + application.name + ' has been deleted');
      this.$state.go('applications.list', {}, {reload: true});
    });
  }

  reset() {
    this.application = _.cloneDeep(this.initialApplication);
    this.$scope.formApplication.$setPristine();
  }

  loadApplicationGroups() {
    if (this.UserService.isUserInRoles(['ADMIN'])) {
      this.GroupService.list("APPLICATION").then((groups) => {
        this.groups = _.union(
          [this.GroupService.getEmptyGroup()],
          groups.data);
      });
    }
  }

  showDeleteApplicationConfirm(ev) {
    var confirm = this.$mdDialog.confirm()
      .title('Would you like to delete your application?')
      .ariaLabel('delete-application')
      .ok('OK')
      .cancel('Cancel')
      .targetEvent(ev);
    var self = this;
    this.$mdDialog.show(confirm).then(function() {
      self.delete(self.application);
    }, function() {
      self.$mdDialog.cancel();
    });
  }
}

export default ApplicationGeneralController;
