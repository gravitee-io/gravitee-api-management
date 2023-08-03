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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';

export class ApiPortalSubscriptionListHarness extends ComponentHarness {
  static hostSelector = 'api-portal-subscription-list';

  public getPlanSelectInput = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="planIds"]' }));
  public getApplicationSelectInput = this.locatorFor(GioFormTagsInputHarness);
  public getStatusSelectInput = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="statuses"]' }));
  public getApiKeyInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="apiKey"]' }));
  public getCreateSubscriptionButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Create a subscription"]' }));
  public getResetFilterButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Reset filters"]' }));
  public getExportButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Export subscriptions list"]' }));

  public async getApplications(): Promise<string[]> {
    const matSelectHarness = await this.getApplicationSelectInput();
    return matSelectHarness.getTags();
  }
}
