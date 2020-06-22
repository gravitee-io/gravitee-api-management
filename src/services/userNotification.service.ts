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

import { User } from '../entities/user';
import { PagedResult } from '../entities/pagedResult';
import { UserNotification } from '../entities/userNotification';
import { IHttpPromise } from 'angular';

class UserNotificationService {
  private URL: string;
  private Constants: any;

  constructor(private $http: ng.IHttpService, Constants) {
    'ngInject';
    this.Constants = Constants;
    this.URL = this.Constants.orgBaseURL + '/user/notifications';
  }

  getNotificationSchedulerInSeconds(): number {
    if (this.Constants.scheduler && this.Constants.scheduler.notifications) {
      return this.Constants.scheduler.notifications;
    }
    return 10;
  }

  getNotifications(): IHttpPromise<any> {
    const config = { ignoreLoadingBar: true, silentCall: true } as ng.IRequestShortcutConfig;
    return this.$http.get(this.URL, config);
  }

  delete(notification: UserNotification): IHttpPromise<any> {
    return this.$http.delete(this.URL + '/' + notification.id);
  }

  deleteAll(): IHttpPromise<any> {
    return this.$http.delete(this.URL);
  }

  fillUserNotifications(user: User, notifications: PagedResult) {
    user.notifications.metadata = notifications.metadata;
    user.notifications.data = notifications.data;
    user.notifications.page = notifications.page;
  }
}

export default UserNotificationService;
