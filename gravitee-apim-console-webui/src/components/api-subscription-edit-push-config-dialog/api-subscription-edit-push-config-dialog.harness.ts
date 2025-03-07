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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export interface ApiSubscriptionEditPushConfigDialogHarnessOptions extends BaseHarnessFilters {}

export class ApiSubscriptionEditPushConfigDialogHarness extends ComponentHarness {
  public static readonly hostSelector = `api-subscription-edit-push-config-dialog`;

  public static with(
    options: ApiSubscriptionEditPushConfigDialogHarnessOptions,
  ): HarnessPredicate<ApiSubscriptionEditPushConfigDialogHarness> {
    return new HarnessPredicate(ApiSubscriptionEditPushConfigDialogHarness, options);
  }

  private channelInput = this.locatorFor(MatInputHarness.with({ selector: 'input[formcontrolname="channel"]' }));

  public async getChannelValue(): Promise<string> {
    const channelInput = await this.channelInput();
    return channelInput.getValue();
  }

  public async setChannelInput(value: string): Promise<void> {
    const channelInput = await this.channelInput();
    return channelInput.setValue(value);
  }

  public async close(): Promise<void> {
    const closeButton = await this.locatorFor(MatButtonHarness.with({ text: /Close/ }))();
    await closeButton.click();
  }

  public async save(): Promise<void> {
    const confirmButton = await this.locatorFor(MatButtonHarness.with({ text: /Save/ }))();
    await confirmButton.click();
  }
}
