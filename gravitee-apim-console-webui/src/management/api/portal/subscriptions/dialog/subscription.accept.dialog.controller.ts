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
import moment from 'moment';
import * as angular from 'angular';

class DialogSubscriptionAcceptController {
  private now: Date;
  private startingAt: Date;
  private endingAt: Date;
  private customApiKey: string;
  private customApiKeyInputState: string;
  private reason: string;

  constructor(
    private apiId: string,
    private applicationId: string,
    private canUseCustomApiKey: boolean,
    private sharedApiKeyMode: boolean,
    private $mdDialog: angular.material.IDialogService,
  ) {
    'ngInject';
    this.now = moment().toDate();
    this.onApiKeyValueChange = this.onApiKeyValueChange.bind(this);
  }

  hide() {
    this.$mdDialog.cancel();
  }

  save() {
    this.$mdDialog.hide({
      starting_at: this.startingAt,
      ending_at: this.endingAt,
      reason: this.reason,
      customApiKey: this.customApiKey,
    });
  }

  onApiKeyValueChange = (apiKeyValidatedInput) => {
    this.customApiKey = apiKeyValidatedInput.customApiKey;
    this.customApiKeyInputState = apiKeyValidatedInput.customApiKeyInputState;
  };
}

export default DialogSubscriptionAcceptController;
