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
import UserService from "../../../services/user.service";
import {IScope} from "angular";
export const NavbarComponent: ng.IComponentOptions = {
  template: require('./navbar.html'),
  controller: function(UserService: UserService, $scope: IScope, Constants) {
    'ngInject';

    const vm = this;

    $scope.$on('graviteeUserRefresh', function () {
      UserService.current().then(function (user) {
        vm.graviteeUser = user;
        if (user && user.username) {
          let that = vm;
          UserService.currentUserPicture().then( (picture) => {
            that.graviteeUser.picture = picture;
          });
        }
      });
    });

    vm.$onInit = function () {
      $scope.$emit('graviteeUserRefresh');
    };

    vm.isUserManagement = function () {
      return vm.graviteeUser.isAdmin();
    };

    vm.getLogo = function() {
      return Constants.theme.logo;
    };

    vm.getUserPicture = function() {
      return 'assets/default_user_picture.png';
    };
  }
};
