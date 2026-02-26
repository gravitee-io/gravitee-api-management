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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { chunk } from 'lodash';
import { MatButtonHarness } from '@angular/material/button/testing';

import { SubscriptionApiKeysHarness } from '../components/subscription-api-keys/subscription-api-keys.harness';

export class ApplicationSubscriptionHarness extends ComponentHarness {
  static readonly hostSelector = 'application-subscription';

  private getSubscriptionDetailsToChunk = () => this.locatorForAll(`dt, dd`)();

  public getSubscriptionApiKeysHarness = this.locatorForOptional(SubscriptionApiKeysHarness);

  async getSubscriptionDetails(): Promise<string[][]> {
    const subscriptionDetails = await this.getSubscriptionDetailsToChunk();

    const subscriptionDetailsToChunk = await parallel(() => subscriptionDetails.map(async detail => await detail.text()));
    return chunk(subscriptionDetailsToChunk, 2);
  }

  async getMetadata(): Promise<string> {
    const el = await this.locatorFor('[data-testid="subscription-metadata"]')();
    return (await el.text()).trim();
  }

  async metadataEditorIsVisible(): Promise<boolean> {
    return this.locatorForOptional('.subscription__metadata-editor')().then(el => el !== null);
  }

  async closeSubscription(): Promise<void> {
    const button = await this.locatorFor(MatButtonHarness.with({ text: /Close/ }))();
    return button.click();
  }
}
