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
  controller: function(UserService: UserService, $scope: IScope, Constants, $rootScope: IScope, $state: ng.ui.IStateService, $transitions) {
    'ngInject';

    const vm = this;
    vm.$rootScope = $rootScope;
    vm.displayContextualDocumentationButton = false;
    vm.visible = true;

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

      vm.supportEnabled = Constants.support && Constants.support.enabled;
    });

    $transitions.onFinish({}, function (trans) {
      vm.displayContextualDocumentationButton =
        !trans.to().name.startsWith('portal') &&
        !trans.to().name.startsWith('support') &&
        !trans.to().name.startsWith('login') &&
        !trans.to().name.startsWith('registration') &&
        !trans.to().name.startsWith('user');

      let forceLogin = (Constants.authentication && Constants.authentication.forceLogin) || false;
      vm.visible = ! forceLogin || (forceLogin && !trans.to().name.startsWith('login') &&
        !trans.to().name.startsWith('registration'));
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

    vm.openContextualDocumentation = function() {
      vm.$rootScope.$broadcast('openContextualDocumentation');
    };
  }
};
