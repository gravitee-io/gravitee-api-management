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
import { StateService } from '@uirouter/core';

import { UserType } from './org-settings-new-user.component';

import NotificationService from '../../../../services/notification.service';
import UserService from '../../../../services/user.service';

const NewUserComponent: ng.IComponentOptions = {
  template: require('./new-user.html'),
  bindings: {
    identityProviders: '<',
  },
  controller: function (UserService: UserService, NotificationService: NotificationService, $state: StateService) {
    'ngInject';

    this.$onInit = () => {
      this.types = [
        {
          title: 'User',
          id: 'EXTERNAL_USER',
          icon: 'social:person_add',
        },
        {
          title: 'Service Account',
          id: 'SERVICE_ACCOUNT',
          icon: 'communication:shield-user',
        },
      ];

      this.userType = UserType.EXTERNAL_USER;

      if (this.identityProviders && this.identityProviders.length) {
        this.identityProviders.unshift({ id: 'gravitee', name: 'Gravitee' });
        this.user = { source: 'gravitee' };
      }
    };

    this.changeType = (event) => {
      this.userType = event.detail.id;
      this.user = {};
      this.formCreation?.$setPristine();
      this.formCreation?.$setUntouched();
      this.formServiceCreation?.$setPristine();
      this.formServiceCreation?.$setUntouched();
    };

    this.create = () => {
      let toCreate = this.user;

      if (this.userType === UserType.SERVICE_ACCOUNT) {
        toCreate = {
          ...this.user,
          service: true,
          source: 'gravitee',
        };

        delete toCreate.firstname;
      }

      UserService.create(toCreate).then(() => {
        NotificationService.show('User registered with success');
        $state.go('organization.settings.ajs-users');
      });
    };

    this.isServiceUser = () => this.userType === UserType.SERVICE_ACCOUNT;
  },
};

export default NewUserComponent;
