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

import { DivHarness } from '../../../../testing/div.harness';

export class ApiTabToolsComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-api-tab-tools';

  private readonly getEmptyToolsMessage = this.locatorForOptional(
    DivHarness.with({ selector: '[data-testid="api-tab-tools-empty-message"]' }),
  );

  async getEmptyToolsMessageText(): Promise<string | null> {
    const el = await this.getEmptyToolsMessage();
    if (!el) {
      return null;
    }
    const text = await el.getText();
    return text?.trim() ?? null;
  }
}
