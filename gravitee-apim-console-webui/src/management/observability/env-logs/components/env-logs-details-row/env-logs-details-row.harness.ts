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

export class EnvLogsDetailsRowHarness extends ComponentHarness {
  static hostSelector = 'env-logs-details-row';

  private readonly label = this.locatorFor('dt');
  private readonly content = this.locatorFor('dd');

  async getLabel(): Promise<string> {
    return (await this.label()).text();
  }

  async getContent(): Promise<string> {
    return (await this.content()).text();
  }

  async getDataTestId(): Promise<string | null> {
    const dd = await this.content();
    return dd.getAttribute('data-testid');
  }
}
