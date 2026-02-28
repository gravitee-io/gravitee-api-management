/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness } from '@angular/cdk/testing';

import { RadioCardHarness } from '../../../../components/radio-card/radio-card.harness';

export class SubscribeToApiCheckoutHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-subscribe-to-api-checkout';
  protected locateApiKeyMode = this.locatorForOptional('.subscribe-to-api-checkout__api-key-mode');
  protected locateSubscriptionForm = this.locatorForOptional('.subscribe-to-api-checkout__form');

  public async getGeneratedApiKeyRadio(): Promise<RadioCardHarness> {
    return await this.locateRadioCard('Generated API Key');
  }

  public async getSharedApiKeyRadio(): Promise<RadioCardHarness> {
    return await this.locateRadioCard('Shared API Key');
  }

  public async isChooseApiKeyModeVisible(): Promise<boolean> {
    return await this.locateApiKeyMode().then(res => !!res);
  }

  public async isSubscriptionFormVisible(): Promise<boolean> {
    return await this.locateSubscriptionForm().then(res => !!res);
  }

  protected locateRadioCard = (title: string) => this.locatorFor(RadioCardHarness.with({ title }))();
}
