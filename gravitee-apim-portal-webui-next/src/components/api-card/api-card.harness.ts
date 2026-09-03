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

export class ApiCardHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-api-card';
  protected locateCard = this.locatorFor('.api-card');
  protected locateOpen = this.locatorFor('.api-card__open');
  protected locateTitle = this.locatorFor('.next-gen-h5');
  protected locateDescription = this.locatorFor('.api-card__description');
  protected locateTypeBadge = this.locatorForOptional('[data-testid="api-type-badge"]');
  protected locateVersion = this.locatorFor('[data-testid="api-card-version"]');
  protected locateAccess = this.locatorForOptional('[data-testid="api-access-token"]');
  protected locateMore = this.locatorFor('[data-testid="api-card-more"]');
  protected locateCapabilities = this.locatorForAll('app-overflow-labels [data-testid="visible-badge"]');
  protected locateCapabilityOverflow = this.locatorForOptional('app-overflow-labels [data-testid="overflow-counter"]');
  protected locatePublished = this.locatorForOptional('[data-testid="api-card-published"]');

  public static with(options: BaseHarnessFilters): HarnessPredicate<ApiCardHarness> {
    return new HarnessPredicate(ApiCardHarness, options);
  }

  public async getTitle(): Promise<string> {
    return (await this.locateTitle()).text({ exclude: '[data-testid="api-card-version"]' });
  }

  public async select(): Promise<void> {
    return (await this.locateOpen()).click();
  }

  public async getDescription(): Promise<string> {
    return (await this.locateDescription()).text();
  }

  public async getVersion(): Promise<string> {
    return (await this.locateVersion()).text();
  }

  public async getAccess(): Promise<string | null> {
    const token = await this.locateAccess();
    return token?.text() ?? null;
  }

  public async getCapabilities(): Promise<string[]> {
    return Promise.all((await this.locateCapabilities()).map(badge => badge.text()));
  }

  public async getCapabilityOverflow(): Promise<string | null> {
    const counter = await this.locateCapabilityOverflow();
    return counter?.text() ?? null;
  }

  public async toggleDetails(): Promise<void> {
    return (await this.locateMore()).click();
  }

  public async isExpanded(): Promise<boolean> {
    return (await this.locateMore()).getAttribute('aria-expanded').then(value => value === 'true');
  }

  public async hasDetailsContent(): Promise<boolean> {
    return (await this.locatorForOptional('.api-card__facts')()) !== null;
  }

  public async getEndpoint(): Promise<string> {
    return (await this.locatorFor('.api-card__fact--wide .api-card__fact__value')()).text();
  }

  public async getSkills(): Promise<string[][]> {
    const rows = await this.locatorForAll('.api-card__skill')();
    return Promise.all(
      rows.map(async row => [
        await (await row.text({ exclude: '.api-card__skill__description' })).trim(),
        await (await row.text({ exclude: '.api-card__skill__name' })).trim(),
      ]),
    );
  }

  public async getFacts(): Promise<string[][]> {
    const facts = await this.locatorForAll('.api-card__fact')();
    return Promise.all(
      facts.map(async fact => [
        (await fact.text({ exclude: '.api-card__fact__value' })).trim(),
        (await fact.text({ exclude: '.api-card__fact__key' })).trim(),
      ]),
    );
  }

  public async clickDetailAction(action: 'subscribe' | 'documentation'): Promise<void> {
    const buttons = await this.locatorForAll('.api-card__actions button')();
    await buttons[action === 'subscribe' ? 0 : 1].click();
  }

  public async getPublished(): Promise<string | null> {
    const published = await this.locatePublished();
    return published?.text() ?? null;
  }

  public async getType(): Promise<string | null> {
    const badge = await this.locateTypeBadge();
    return badge?.text() ?? null;
  }
}
