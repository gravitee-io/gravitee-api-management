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
import { BaseHarnessFilters, ContentContainerComponentHarness, HarnessPredicate } from '@angular/cdk/testing';

export class CatalogApiCardHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-catalog-api-card';
  protected locateCard = this.locatorFor('[data-testid="catalog-api-card"]');

  public static with(options: BaseHarnessFilters): HarnessPredicate<CatalogApiCardHarness> {
    return new HarnessPredicate(CatalogApiCardHarness, options);
  }

  public async getTitle(): Promise<string> {
    return (await (await this.locateCard()).getAttribute('data-api-name')) ?? '';
  }

  public async getType(): Promise<string | null> {
    return (await this.locateCard()).getAttribute('data-api-type');
  }

  public async isMcpServer(): Promise<boolean> {
    return (await (await this.locateCard()).getAttribute('data-api-mcp')) === 'true';
  }

  public async select(): Promise<void> {
    return (await this.locateCard()).click();
  }

  public async clickNotify(): Promise<void> {
    const button = await this.locatorForOptional('[data-testid="api-card-notify"]')();
    if (!button) {
      throw new Error('Notify button not found');
    }
    return button.click();
  }

  public async hasNotifyButton(): Promise<boolean> {
    return (await this.locatorForOptional('[data-testid="api-card-notify"]')()) !== null;
  }
}
