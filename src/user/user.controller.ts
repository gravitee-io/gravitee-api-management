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
import NotificationService from '../services/notification.service';
import { User } from "../entities/user";
import {IScope} from 'angular';
import UserService from "../services/user.service";
import { StateService } from '@uirouter/core';
import StringService from "../services/string.service";

interface IUserScope extends ng.IScope {
  formUser: any;
}

class UserController {
  private originalPicture: any;
  private user: User;

  constructor(
    private UserService: UserService,
    private NotificationService: NotificationService,
    private $state: StateService,
    private $scope: IUserScope,
    private $rootScope: IScope) {
    'ngInject';
  }

  $onInit() {
    if (! this.user || (this.user && this.user.id === undefined)) {
      this.$state.go('login', {}, {reload: true, inherit: false});
    } else {
      this.originalPicture = this.getUserPicture();
    }
  }

  save() {
    this.UserService.save(this.user).then((response) => {
      this.user = response.data;
      this.$rootScope.$broadcast('graviteeUserRefresh');
      this.$scope.formUser.$setPristine();
      this.NotificationService.show("User has been updated successfully");
    });
  }

  cancel() {
    delete this.user.picture;
    this.$state.reload();
  }

  getUserPicture() {
    return this.UserService.currentUserPicture();
  }
}

export default UserController;
