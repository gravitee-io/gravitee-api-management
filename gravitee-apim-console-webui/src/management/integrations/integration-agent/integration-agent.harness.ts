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

import { AsyncFactoryFn, ComponentHarness } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class IntegrationAgentHarness extends ComponentHarness {
  public static readonly hostSelector: string = 'app-integration-agent';

  private refreshStatusButtonLocator: AsyncFactoryFn<MatButtonHarness> = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid=refresh-status-button]' }),
  );

  private accessKeyIdInputLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=accessKeyId-input]' }),
  );

  private secretAccessKeyInputLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=secretAccessKey-input]' }),
  );

  private accordionLocator: AsyncFactoryFn<DivHarness> = this.locatorFor(DivHarness.with({ selector: '[data-testid=accordion-header]' }));

  public async refreshStatus() {
    return this.refreshStatusButtonLocator().then(button => button.click());
  }

  public async setAccessKeyId(str: string): Promise<void> {
    return this.accessKeyIdInputLocator().then((input: MatInputHarness) => input.setValue(str));
  }

  public async setSecretAccessKey(str: string): Promise<void> {
    return this.secretAccessKeyInputLocator().then((input: MatInputHarness) => input.setValue(str));
  }

  public async openAccordion(): Promise<void> {
    return this.accordionLocator()
      .then(el => el.host())
      .then(el => el.click());
  }
}
