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
import {UserNotification} from "../../entities/userNotification";
import UserNotificationService from "../../services/userNotification.service";
import {PagedResult} from "../../entities/pagedResult";
import {IIntervalService, IScope} from "angular";

const PortalNotificationsComponent: ng.IComponentOptions = {
  bindings: {
    user: '<'
  },
  template: require('./portalnotifications.html'),
  controller: function(
      UserNotificationService: UserNotificationService,
      $scope: IScope,
      $interval: IIntervalService) {
    'ngInject';
    const vm = this;
    vm.notificationsScheduler = null;

    vm.$onInit = () => {
      // schedule an automatic refresh of the user notifications
      if (!vm.notificationsScheduler) {
        vm.refreshUserNotifications();
        vm.notificationsScheduler = $interval(() => {
          vm.refreshUserNotifications();
        }, UserNotificationService.getNotificationSchedulerInSeconds() * 1000);
      }
    };

    vm.delete = (notification: UserNotification) => {
      UserNotificationService.delete(notification).then((response) => {
        vm.refreshUserNotifications();
      });
    };

    vm.deleteAll = ($event) => {
      $event.preventDefault();
      UserNotificationService.deleteAll().then((response) => {
        vm.refreshUserNotifications();
      });
    };

    $scope.$on("graviteeUserCancelScheduledServices", function () {
      vm.cancelRefreshUserNotifications();
    });

    vm.cancelRefreshUserNotifications = function() {
      if (vm.notificationsScheduler) {
        $interval.cancel(vm.notificationsScheduler);
        vm.notificationsScheduler = undefined;
      }
    };

    vm.getUserNotificationsCount = () => {
      if (vm.user.notifications) {
        return vm.user.notifications.page.total_elements;
      }
      return 0;
    };

    vm.refreshUserNotifications = () => {
      UserNotificationService.getNotifications().then((response) => {
        const result = new PagedResult();
        result.populate(response.data);
        UserNotificationService.fillUserNotifications(vm.user, result);
      });
    };
  }
};

export default PortalNotificationsComponent;