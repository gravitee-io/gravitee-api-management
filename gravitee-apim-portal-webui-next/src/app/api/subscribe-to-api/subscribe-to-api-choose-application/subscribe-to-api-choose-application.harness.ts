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
import { MatButtonHarness } from '@angular/material/button/testing';

export class SubscribeToApiChooseApplicationHarness extends ComponentHarness {
  public static hostSelector = 'app-subscribe-to-api-choose-application';
  protected locateNoApplications = this.locatorFor('.no-applications');
  protected locateNextPage = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Next page of applications"]' }));
  protected locatePreviousPage = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Previous page of applications"]' }));

  public async noApplicationsMessageShown(): Promise<boolean> {
    return await this.locateNoApplications()
      .then(_ => true)
      .catch(_ => false);
  }

  public async hasNextPageOfApplications(): Promise<boolean> {
    return await this.locateNextPage()
      .then(btn => btn.isDisabled())
      .then(res => !res);
  }

  public async getNextPageOfApplications(): Promise<void> {
    return await this.locateNextPage().then(btn => btn.click());
  }

  public async hasPreviousPageOfApplications(): Promise<boolean> {
    return await this.locatePreviousPage()
      .then(btn => btn.isDisabled())
      .then(res => !res);
  }

  public async getPreviousPageOfApplications(): Promise<void> {
    return await this.locatePreviousPage().then(btn => btn.click());
  }
}
