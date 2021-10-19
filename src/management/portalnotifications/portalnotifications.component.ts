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
import { IIntervalService, IScope } from 'angular';

import { PagedResult } from '../../entities/pagedResult';
import { UserNotification } from '../../entities/userNotification';
import UserNotificationService from '../../services/userNotification.service';

const PortalNotificationsComponent: ng.IComponentOptions = {
  bindings: {
    user: '<',
  },
  template: require('./portalnotifications.html'),
  controller: function (UserNotificationService: UserNotificationService, $scope: IScope, $interval: IIntervalService) {
    'ngInject';
    this.notificationsScheduler = null;

    this.$onInit = () => {
      this.lastNbNotification = -1;
      // schedule an automatic refresh of the user notifications
      if (!this.notificationsScheduler) {
        this.refreshUserNotifications();
        this.notificationsScheduler = $interval(() => {
          this.refreshUserNotifications();
        }, UserNotificationService.getNotificationSchedulerInSeconds() * 1000);
      }
    };

    this.delete = (notification: UserNotification) => {
      this.lastNbNotification--;
      UserNotificationService.delete(notification).then(() => {
        this.refreshUserNotifications();
      });
    };

    this.deleteAll = ($event) => {
      this.lastNbNotification = -1;
      $event.preventDefault();
      UserNotificationService.deleteAll().then(() => {
        this.refreshUserNotifications();
      });
    };

    $scope.$on('graviteeUserCancelScheduledServices', () => {
      this.cancelRefreshUserNotifications();
    });

    this.cancelRefreshUserNotifications = () => {
      if (this.notificationsScheduler) {
        $interval.cancel(this.notificationsScheduler);
        this.notificationsScheduler = undefined;
      }
    };

    this.getUserNotificationsCount = () => {
      if (this.user.notifications) {
        return this.user.notifications.page.total_elements;
      }
      return 0;
    };

    this.refreshUserNotifications = () => {
      UserNotificationService.getNotifications().then((response) => {
        const result = new PagedResult();
        result.populate(response.data);
        UserNotificationService.fillUserNotifications(this.user, result);

        if (this.lastNbNotification < 0) {
          this.lastNbNotification = this.user.notifications.data.length;
        } else {
          if (this.user.notifications.data.length > 0 && this.lastNbNotification < this.user.notifications.data.length) {
            for (let i = this.lastNbNotification; i < this.user.notifications.data.length; i++) {
              this.windowNotification(this.user.notifications.data[i].title, this.user.notifications.data[i].message);
            }
            this.lastNbNotification = this.user.notifications.data.length;
          }
        }
      });
    };

    this.windowNotification = (title: string, message: string) => {
      if ('Notification' in window) {
        Notification.requestPermission().then(() => {
          // eslint:disable-next-line:no-unused-expression
          new Notification(title, { body: message, icon: '/favicon.ico' });
        });
      }
    };
  },
};

export default PortalNotificationsComponent;
