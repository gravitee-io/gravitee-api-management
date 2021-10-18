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

class ConsoleSettingsService {
  private settingsURL: string;

  constructor(private $http, Constants) {
    'ngInject';
    this.settingsURL = `${Constants.org.baseURL}/settings/`;
  }

  save(config) {
    return this.$http.post(this.settingsURL, config);
  }

  get() {
    return this.$http.get(this.settingsURL);
  }

  isReadonly(settings: any, property: string): boolean {
    if (settings && settings.metadata && settings.metadata.readonly) {
      return settings.metadata.readonly.some((key) => key === property);
    }
    return false;
  }
}

export default ConsoleSettingsService;
