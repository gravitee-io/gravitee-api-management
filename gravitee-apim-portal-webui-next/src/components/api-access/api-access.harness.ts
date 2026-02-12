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
import { MatSelectHarness } from '@angular/material/select/testing';

import { CopyCodeHarness } from '../copy-code/copy-code.harness';

export class ApiAccessHarness extends ComponentHarness {
  public static hostSelector = 'app-api-access';

  public async getApiKey(): Promise<string> {
    return await this.locateCopyCodeByTitle('Active API key').then(res => res.getText());
  }

  public async getBaseURL(): Promise<string> {
    return await this.locateCopyCodeByTitle('Base URL').then(res => res.getText());
  }

  public async getBaseURLSelect(): Promise<string | undefined> {
    return await this.locatorForOptional(MatSelectHarness.with({ selector: '#base-urls' }))().then(res => res?.getValueText());
  }

  public async getCommandLine(): Promise<string> {
    return await this.locateCopyCodeById('command-line').then(res => res.getText());
  }

  public async getClientId(): Promise<string> {
    return await this.locateCopyCodeByTitle('Client ID').then(res => res.getText());
  }

  public async getClientSecret(): Promise<string> {
    return await this.locateCopyCodeByTitle('Client Secret').then(res => res.getText());
  }

  public async getProducerCommand(): Promise<string> {
    return await this.locateCopyCodeById('native-kafka-producer').then(res => res.getText());
  }

  public async getPlainConfig(): Promise<string> {
    return await this.locateCopyCodeById('native-kafka-api-key-plain-properties').then(res => res.getText());
  }

  public async getSslConfig(): Promise<string> {
    return await this.locateCopyCodeById('native-kafka-ssl-properties').then(res => res.getText());
  }

  public async toggleClientSecretVisibility(): Promise<void> {
    return await this.locateCopyCodeByTitle('Client Secret').then(res => res.changePasswordVisibility());
  }

  protected locateCopyCodeByTitle = (title: string) => this.locatorFor(CopyCodeHarness.with({ selector: `[title="${title}"]` }))();
  protected locateCopyCodeById = (id: string) => this.locatorFor(CopyCodeHarness.with({ selector: `#${id}` }))();
}
