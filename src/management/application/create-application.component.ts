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
import ApplicationService from "../../services/applications.service";
import NotificationService from "../../services/notification.service";
import { StateService } from '@uirouter/core';

interface IApplicationScope extends ng.IScope {
  formApplication: any;
}

const CreateApplicationComponent: ng.IComponentOptions = {
  template: require('./create-application.html'),
  controller: class {

    private application: any;

    constructor(
      private ApplicationService: ApplicationService,
      private NotificationService: NotificationService,
      private $scope: IApplicationScope,
      private $state: StateService
    ) {
      'ngInject';

      this.application = {};
    }

    create() {
      this.ApplicationService.create(this.application).then((response) => {
        this.NotificationService.show('Application ' + this.application.name + ' has been created');
        this.$state.go('management.applications.application.general', {applicationId: response.data.id}, {reload: true});
      });
    }

    reset() {
      this.application = {};
      this.$scope.formApplication.$setPristine();
    }
  }
};

export default CreateApplicationComponent;
