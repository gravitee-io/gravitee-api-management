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
import UserService from '../services/user.service';
import NotificationService from '../services/notification.service';
import { User } from "../entities/user";

interface IUserScope extends ng.IScope {
  formUser: any;
}

class UserController {
  private originalPicture: any;
  private user: User;

  constructor(
    private UserService: UserService,
    private NotificationService: NotificationService,
    private $state: ng.ui.IStateService,
    private $scope: IUserScope) {
    'ngInject';
  }

  $onInit() {
    if (! this.user || (this.user && this.user.username === undefined)) {
      this.$state.go('login', {}, {reload: true, inherit: false});
    }
  }

  save() {
    this.UserService.save(this.user).then(() => {
      this.$scope.formUser.$setPristine();
      this.originalPicture = this.user.picture;
      this.NotificationService.show("User has been updated successfully");
    });
  }

  cancel() {
    this.user.picture = this.originalPicture;
    delete this.originalPicture;
  }
}

export default UserController;
