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

import { IHttpPromise } from 'angular';

import { PagedResult } from '../entities/pagedResult';
import { User } from '../entities/user';
import { UserNotification } from '../entities/userNotification';

class UserNotificationService {
  constructor(private $http: ng.IHttpService, private Constants) {
    'ngInject';
  }

  getNotificationSchedulerInSeconds(): number {
    if (this.Constants.org.settings.scheduler && this.Constants.org.settings.scheduler.notifications) {
      return this.Constants.org.settings.scheduler.notifications;
    }
    return 10;
  }

  getNotifications(): IHttpPromise<any> {
    const config = { ignoreLoadingBar: true, silentCall: true } as ng.IRequestShortcutConfig;
    return this.$http.get(this.Constants.org.baseURL + '/user/notifications', config);
  }

  delete(notification: UserNotification): IHttpPromise<any> {
    return this.$http.delete(this.Constants.org.baseURL + '/user/notifications' + '/' + notification.id);
  }

  deleteAll(): IHttpPromise<any> {
    return this.$http.delete(this.Constants.org.baseURL + '/user/notifications');
  }

  fillUserNotifications(user: User, notifications: PagedResult) {
    user.notifications.metadata = notifications.metadata;
    user.notifications.data = notifications.data;
    user.notifications.page = notifications.page;
  }
}

export default UserNotificationService;
