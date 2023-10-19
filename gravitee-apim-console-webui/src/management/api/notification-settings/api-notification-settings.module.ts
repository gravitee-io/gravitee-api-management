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
import { NgModule } from '@angular/core';

import { ApiNotificationSettingsListComponent } from './notification-settings-list/api-notification-settings-list.component';
import { ApiNotificationSettingsDetailsComponent } from './notofication-settings-details/api-notification-settings-details.component';

import { NotificationSettingsListModule } from '../../../components/notifications/notifications-list/notification-settings-list.module';

@NgModule({
  declarations: [ApiNotificationSettingsListComponent, ApiNotificationSettingsDetailsComponent],
  exports: [ApiNotificationSettingsListComponent, ApiNotificationSettingsDetailsComponent],
  imports: [NotificationSettingsListModule],
})
export class ApiNotificationSettingsModule {}
