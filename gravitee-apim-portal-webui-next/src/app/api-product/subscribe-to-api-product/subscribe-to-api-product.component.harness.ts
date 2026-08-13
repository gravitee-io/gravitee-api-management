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
import { ContentContainerComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { PlanCardHarness } from '../../../components/subscribe/plan-card/plan-card.harness';

export class SubscribeToApiProductHarness extends ContentContainerComponentHarness {
  static readonly hostSelector = 'app-subscribe-to-api-product';

  private readonly locateNextButton = this.locatorForOptional(MatButtonHarness.with({ text: 'Next' }));
  private readonly locateSubscribeButton = this.locatorForOptional(MatButtonHarness.with({ text: /Subscribe|Subscribing/ }));
  private readonly locateConfirmation = this.locatorForOptional('[data-testid="subscription-confirmation"]');
  private readonly locateError = this.locatorForOptional('[role="alert"]');

  async getText(): Promise<string> {
    return (await this.host()).text();
  }

  async selectPlan(planId: string): Promise<void> {
    const plan = await this.locatorForOptional(PlanCardHarness.with({ selector: `#${planId}` }))();
    if (!plan) {
      throw new Error(`Plan ${planId} not found`);
    }
    await plan.select();
  }

  async goToNextStep(): Promise<void> {
    const button = await this.locateNextButton();
    if (!button) {
      throw new Error('Next button not found');
    }
    await button.click();
  }

  async subscribe(): Promise<void> {
    const button = await this.locateSubscribeButton();
    if (!button) {
      throw new Error('Subscribe button not found');
    }
    await button.click();
  }

  async hasSubscribeAction(): Promise<boolean> {
    return (await this.locateSubscribeButton()) !== null;
  }

  async hasConfirmation(): Promise<boolean> {
    return (await this.locateConfirmation()) !== null;
  }

  async hasError(): Promise<boolean> {
    return (await this.locateError()) !== null;
  }
}
