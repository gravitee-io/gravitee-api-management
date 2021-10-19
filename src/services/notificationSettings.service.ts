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

import { NotificationConfig } from '../entities/notificationConfig';
import { Scope } from '../entities/scope';

class NotificationSettingsService {
  constructor(private $http: ng.IHttpService, private Constants) {
    'ngInject';
  }

  getHooks(scope: Scope): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.get(`${this.Constants.env.baseURL}/applications/` + 'hooks');
      case Scope.API:
        return this.$http.get(`${this.Constants.env.baseURL}/apis/` + 'hooks');
      case Scope.PORTAL:
        return this.$http.get(`${this.Constants.env.baseURL}/configuration/` + 'hooks');
      default:
        break;
    }
  }

  getNotifiers(scope: Scope, id: string): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.get(`${this.Constants.env.baseURL}/applications/` + id + '/notifiers');
      case Scope.API:
        return this.$http.get(`${this.Constants.env.baseURL}/apis/` + id + '/notifiers');
      case Scope.PORTAL:
        return this.$http.get(`${this.Constants.env.baseURL}/configuration/` + 'notifiers');
      default:
        break;
    }
  }

  getNotificationSettings(scope: Scope, id: string): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.get(`${this.Constants.env.baseURL}/applications/` + id + '/notificationsettings');
      case Scope.API:
        return this.$http.get(`${this.Constants.env.baseURL}/apis/` + id + '/notificationsettings');
      case Scope.PORTAL:
        return this.$http.get(`${this.Constants.env.baseURL}/configuration/` + 'notificationsettings');
      default:
        break;
    }
  }

  delete(scope: Scope, referenceId: string, notificationSettingId: string): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.delete(
          `${this.Constants.env.baseURL}/applications/` + referenceId + '/notificationsettings/' + notificationSettingId,
        );
      case Scope.API:
        return this.$http.delete(`${this.Constants.env.baseURL}/apis/` + referenceId + '/notificationsettings/' + notificationSettingId);
      case Scope.PORTAL:
        return this.$http.delete(`${this.Constants.env.baseURL}/configuration/` + 'notificationsettings/' + notificationSettingId);
      default:
        break;
    }
  }

  update(cfg: NotificationConfig): IHttpPromise<any> {
    const urlId = cfg.id ? cfg.id : '';
    if (cfg.referenceType === 'API') {
      return this.$http.put(`${this.Constants.env.baseURL}/apis/` + cfg.referenceId + '/notificationsettings/' + urlId, cfg);
    } else if (cfg.referenceType === 'APPLICATION') {
      return this.$http.put(`${this.Constants.env.baseURL}/applications/` + cfg.referenceId + '/notificationsettings/' + urlId, cfg);
    } else if (cfg.referenceType === 'PORTAL') {
      return this.$http.put(`${this.Constants.env.baseURL}/configuration/` + 'notificationsettings/' + urlId, cfg);
    } else {
      return;
    }
  }

  create(cfg: NotificationConfig): IHttpPromise<any> {
    if (cfg.referenceType === 'API') {
      return this.$http.post(`${this.Constants.env.baseURL}/apis/` + cfg.referenceId + '/notificationsettings', cfg);
    } else if (cfg.referenceType === 'APPLICATION') {
      return this.$http.post(`${this.Constants.env.baseURL}/applications/` + cfg.referenceId + '/notificationsettings', cfg);
    } else if (cfg.referenceType === 'PORTAL') {
      return this.$http.post(`${this.Constants.env.baseURL}/configuration/` + 'notificationsettings', cfg);
    } else {
      return;
    }
  }
}

export default NotificationSettingsService;
