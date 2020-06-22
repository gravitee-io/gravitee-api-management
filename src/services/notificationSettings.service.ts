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

import { Scope } from '../entities/scope';
import { IHttpPromise } from 'angular';
import { NotificationConfig } from '../entities/notificationConfig';

class NotificationSettingsService {
  private Constants: any;
  private applicationsURL: string;
  private apisURL: string;
  private portalgCfgURL: string;

  constructor(private $http: ng.IHttpService, Constants) {
    'ngInject';
    this.Constants = Constants;
    this.applicationsURL = `${Constants.envBaseURL}/applications/`;
    this.apisURL = `${Constants.envBaseURL}/apis/`;
    this.portalgCfgURL = `${Constants.envBaseURL}/configuration/`;
  }

  getHooks(scope: Scope): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.get(this.applicationsURL + 'hooks');
      case Scope.API:
        return this.$http.get(this.apisURL + 'hooks');
      case Scope.PORTAL:
        return this.$http.get(this.portalgCfgURL + 'hooks');
      default:
        break;
    }
  }

  getNotifiers(scope: Scope, id: string): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.get(this.applicationsURL + id + '/notifiers');
      case Scope.API:
        return this.$http.get(this.apisURL + id + '/notifiers');
      case Scope.PORTAL:
        return this.$http.get(this.portalgCfgURL + 'notifiers');
      default:
        break;
    }
  }

  getNotificationSettings(scope: Scope, id: string): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.get(this.applicationsURL + id + '/notificationsettings');
      case Scope.API:
        return this.$http.get(this.apisURL + id + '/notificationsettings');
      case Scope.PORTAL:
        return this.$http.get(this.portalgCfgURL + 'notificationsettings');
      default:
        break;
    }
  }

  delete(scope: Scope, referenceId: string, notificationSettingId: string): IHttpPromise<any> {
    switch (scope) {
      case Scope.APPLICATION:
        return this.$http.delete(this.applicationsURL + referenceId + '/notificationsettings/' + notificationSettingId);
      case Scope.API:
        return this.$http.delete(this.apisURL + referenceId + '/notificationsettings/' + notificationSettingId);
      case Scope.PORTAL:
        return this.$http.delete(this.portalgCfgURL + 'notificationsettings/' + notificationSettingId);
      default:
        break;
    }
  }

  update(cfg: NotificationConfig): IHttpPromise<any> {
    const urlId = (cfg.id ? cfg.id : '');
    if (cfg.referenceType === 'API') {
      return this.$http.put(this.apisURL + cfg.referenceId + '/notificationsettings/' + urlId, cfg);
    } else if (cfg.referenceType === 'APPLICATION') {
      return this.$http.put(this.applicationsURL + cfg.referenceId + '/notificationsettings/' + urlId, cfg);
    } else if (cfg.referenceType === 'PORTAL') {
      return this.$http.put(this.portalgCfgURL + 'notificationsettings/' + urlId, cfg);
    } else {
      return;
    }
  }

  create(cfg: NotificationConfig): IHttpPromise<any> {
    if (cfg.referenceType === 'API') {
      return this.$http.post(this.apisURL + cfg.referenceId + '/notificationsettings', cfg);
    } else if (cfg.referenceType === 'APPLICATION') {
      return this.$http.post(this.applicationsURL + cfg.referenceId + '/notificationsettings', cfg);
    } else if (cfg.referenceType === 'PORTAL') {
      return this.$http.post(this.portalgCfgURL + 'notificationsettings', cfg);
    } else {
      return;
    }
  }
}

export default NotificationSettingsService;
