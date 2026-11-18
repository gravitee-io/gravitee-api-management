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
import { MatInputHarness } from '@angular/material/input/testing';

export class ApplicationSharedApiKeyRenewDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'application-shared-api-key-renew-dialog';

  private readonly notice = this.locatorFor('[data-testid="shared-api-key-renew-notice"]');
  private readonly valueInput = this.locatorForOptional(MatInputHarness.with({ selector: '[data-testid="shared-api-key-renew-value"]' }));
  private readonly submitButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="shared-api-key-renew-submit"]' }));
  private readonly error = this.locatorForOptional('[data-testid="shared-api-key-renew-error"]');

  public async getNotice(): Promise<string> {
    return (await this.notice()).text();
  }

  public async hasValueField(): Promise<boolean> {
    return (await this.valueInput()) !== null;
  }

  public async getError(): Promise<string | null> {
    const error = await this.error();
    return error ? error.text() : null;
  }

  public async setValue(value: string): Promise<void> {
    const input = await this.valueInput();
    await input.setValue(value);
  }

  public async isSubmitDisabled(): Promise<boolean> {
    return (await this.submitButton()).isDisabled();
  }

  public async submit(): Promise<void> {
    return (await this.submitButton()).click();
  }
}
