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

import ApiService from '../../../../../services/api.service';

export enum CustomApiKeyInputState {
  EMPTY = 'empty',
  VALID = 'valid',
  INVALID = 'invalid',
}

const ApiKeyValidatedInput: ng.IComponentOptions = {
  bindings: {
    apiId: '<',
    formReference: '<',
    label: '<',
    onChange: '<',
  },
  template: require('./apiKeyValidatedInput.html'),
  controller: function (ApiService: ApiService) {
    'ngInject';

    this.value = '';
    this.state = CustomApiKeyInputState.EMPTY;

    this.$onInit = () => {
      this.label = this.label || 'Custom API key';
    };

    this.valueChange = function () {
      this.checkApiKeyUnicity(this.value);
    };

    this.checkApiKeyUnicity = (apiKey: string) => {
      if (apiKey && apiKey.length > 0) {
        ApiService.verifyApiKey(this.apiId, apiKey)
          .then(
            (response) => {
              if (response && response.data) {
                this.state = CustomApiKeyInputState.VALID;
              } else {
                this.state = CustomApiKeyInputState.INVALID;
              }
            },
            () => {
              this.state = CustomApiKeyInputState.INVALID;
            },
          )
          .then(() => this.emitChanges());
      } else {
        this.state = CustomApiKeyInputState.EMPTY;
        this.emitChanges();
      }
    };

    this.isApiKeyValid = () => {
      return this.state === CustomApiKeyInputState.VALID;
    };

    this.isApiKeyInvalid = () => {
      return this.state === CustomApiKeyInputState.INVALID;
    };

    this.emitChanges = () => {
      this.onChange({
        customApiKey: this.value,
        customApiKeyInputState: this.state,
      });
    };
  },
};

export default ApiKeyValidatedInput;
