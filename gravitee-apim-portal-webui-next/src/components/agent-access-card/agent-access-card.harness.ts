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

import { CopyCodeHarness } from '../copy-code/copy-code.harness';

export class AgentAccessCardComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-agent-access-card';

  private readonly getApplicationName = this.locatorForOptional('[data-testid="application-name"]');
  private readonly getApiKey = this.locatorForOptional(CopyCodeHarness.with({ selector: '#api-key' }));
  private readonly getBaseUrl = this.locatorForOptional(CopyCodeHarness.with({ selector: '#base-url' }));
  private readonly getCurl = this.locatorForOptional(CopyCodeHarness.with({ selector: '#command-line' }));
  private readonly getClientId = this.locatorForOptional(CopyCodeHarness.with({ selector: '#client-id' }));
  private readonly getClientSecret = this.locatorForOptional(CopyCodeHarness.with({ selector: '#client-secret' }));

  async getHostText(): Promise<string> {
    return (await this.host()).text();
  }

  async getApplicationNameText(): Promise<string | null> {
    const name = await this.getApplicationName();
    return name ? name.text() : null;
  }

  async getApiKeyText(): Promise<string | null> {
    const copyCode = await this.getApiKey();
    return copyCode ? copyCode.getText() : null;
  }

  async getBaseUrlText(): Promise<string | null> {
    const copyCode = await this.getBaseUrl();
    return copyCode ? copyCode.getText() : null;
  }

  async getCurlText(): Promise<string | null> {
    const copyCode = await this.getCurl();
    return copyCode ? copyCode.getText() : null;
  }

  async getClientIdText(): Promise<string | null> {
    const copyCode = await this.getClientId();
    return copyCode ? copyCode.getText() : null;
  }

  async getClientSecretText(): Promise<string | null> {
    const copyCode = await this.getClientSecret();
    return copyCode ? copyCode.getText() : null;
  }
}
