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
import { NotificationSettings } from './notificationSettings';

export function fakeNotificationSettings(attributes?: Partial<NotificationSettings>): NotificationSettings {
  const defaultValue: NotificationSettings = {
    id: 'f7889b1c-2b4c-435d-889b-1c2b4c235da9',
    name: 'test tes test',
    referenceType: 'API',
    referenceId: 'f1ddf4b5-c23a-33a7-87bf-28ec0a1d9db9',
    notifier: 'default-email',
    hooks: [],
    useSystemProxy: false,
    config_type: 'GENERIC',
  };
  return {
    ...defaultValue,
    ...attributes,
  };
}
