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
class ErrorController {
  private error: Record<string, any>;

  constructor() {
    'ngInject';
  }

  $onChanges = function (changesObj) {
    if (changesObj.error != null && this.error != null) {
      this.title = this.error.title || '';
      try {
        this.messages = JSON.parse(this.error.message);
      } catch (e) {
        if (Array.isArray(this.error.message)) {
          this.messages = this.error.message;
        } else {
          this.messages = [this.error.message];
        }
      }
    } else {
      this.title = null;
      this.messages = null;
    }
  };

  hasErrors = function () {
    return this.title != null || (this.messages != null && this.messages.length > 0);
  };
}

export default ErrorController;
