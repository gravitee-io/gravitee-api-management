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

export class ApiAccessHarness extends ComponentHarness {
  public static hostSelector = 'app-api-access';

  public async getApiKey(): Promise<string> {
    return await this.locateCopyCodeByTitle('API Key').then(res => res.getText());
  }

  public async getBaseURL(): Promise<string> {
    return await this.locateCopyCodeByTitle('Base URL').then(res => res.getText());
  }

  public async getCommandLine(): Promise<string> {
    return await this.locateCopyCodeByTitle('Command Line').then(res => res.getText());
  }

  public async getClientId(): Promise<string> {
    return await this.locateCopyCodeByTitle('Client ID').then(res => res.getText());
  }

  public async getClientSecret(): Promise<string> {
    return await this.locateCopyCodeByTitle('Client Secret').then(res => res.getText());
  }

  public async toggleClientSecretVisibility(): Promise<void> {
    return await this.locateCopyCodeByTitle('Client Secret').then(res => res.changePasswordVisibility());
  }

  protected locateCopyCodeByTitle = (title: string) => this.locatorFor(CopyCodeHarness.with({ selector: `[title="${title}"]` }))();
}
