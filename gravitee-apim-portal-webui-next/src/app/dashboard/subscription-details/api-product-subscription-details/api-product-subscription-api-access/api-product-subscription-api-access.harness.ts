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

export class ApiProductSubscriptionApiAccessHarness extends ComponentHarness {
  static hostSelector = 'app-api-product-subscription-api-access';

  private readonly documentationLink = this.locatorForOptional('a');
  private readonly copyCodeBlocks = this.locatorForAll('app-copy-code');

  async getText(): Promise<string> {
    return (await this.host()).text();
  }

  async getDocumentationLink(): Promise<string | null> {
    return (await this.documentationLink())?.getAttribute('href') ?? null;
  }

  async getCopyCodeCount(): Promise<number> {
    return (await this.copyCodeBlocks()).length;
  }
}
