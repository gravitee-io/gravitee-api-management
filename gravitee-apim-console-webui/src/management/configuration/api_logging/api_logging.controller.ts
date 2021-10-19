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
import ConsoleSettingsService from '../../../services/consoleSettings.service';
import NotificationService from '../../../services/notification.service';

class ApiLoggingController {
  public providedConfigurationMessage = 'Configuration provided by the system';
  private formApiLogging: any;
  private settings: any;

  constructor(private ConsoleSettingsService: ConsoleSettingsService, private NotificationService: NotificationService) {
    'ngInject';
  }

  save() {
    this.ConsoleSettingsService.save(this.settings).then(() => {
      this.NotificationService.show('API logging saved');
      this.formApiLogging.$setPristine();
    });
  }

  isReadonlySetting(property: string): boolean {
    return this.ConsoleSettingsService.isReadonly(this.settings, property);
  }
}

export default ApiLoggingController;
