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
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatChipSetHarness } from '@angular/material/chips/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class EnvLogsFilterBarHarness extends ComponentHarness {
  static hostSelector = 'env-logs-filter-bar';

  async getSelects(): Promise<MatSelectHarness[]> {
    return this.locatorForAll(MatSelectHarness)();
  }

  async getRefreshButton(): Promise<MatButtonHarness> {
    return this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="refresh-button"]' }))();
  }

  async getMoreButton(): Promise<MatButtonHarness> {
    return this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="more-button"]' }))();
  }

  async getChipSet(): Promise<MatChipSetHarness | null> {
    return this.locatorForOptional(MatChipSetHarness)();
  }

  async getNoFiltersText(): Promise<string | null> {
    const el = await this.locatorForOptional('[data-testid="no-filters-text"]')();
    return el ? el.text() : null;
  }

  async getResetButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="reset-filters-button"]' }))();
  }
}
