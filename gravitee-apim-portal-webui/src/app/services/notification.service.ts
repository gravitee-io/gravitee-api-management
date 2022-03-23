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
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { Notification } from '../model/notification';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private notificationSource = new BehaviorSubject<Notification>(undefined);
  notification = this.notificationSource.asObservable();

  success(code: string, parameters?: object, fallbackMessage?: string, closable?: boolean) {
    this.notify('success', code, parameters, fallbackMessage, closable);
  }

  error(code: string, parameters?: object, fallbackMessage?: string, closable?: boolean) {
    this.notify('error', code, parameters, fallbackMessage, closable);
  }

  info(code: string, parameters?: object, fallbackMessage?: string, closable?: boolean) {
    this.notify('info', code, parameters, fallbackMessage, closable);
  }

  warning(code: string, parameters?: object, fallbackMessage?: string, closable?: boolean) {
    this.notify('warning', code, parameters, fallbackMessage, closable);
  }

  private notify(type: string, code: string, parameters: object, message?: string, closable?: boolean) {
    const notif = new Notification();
    notif.type = type;
    notif.code = code;
    notif.parameters = parameters;
    notif.message = message;
    notif.closable = closable !== undefined ? closable : true;
    this.notificationSource.next(notif);
  }

  reset() {
    this.notificationSource.next(undefined);
  }
}
