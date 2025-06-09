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
import { MatChipHarness } from '@angular/material/chips/testing';

export class ApiCardHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-api-card';
  protected locateHeaderContent = this.locatorFor('.api-card__header__content');
  protected locateDescription = this.locatorFor('.api-card__description');
  protected locateMcpServerChip = this.locatorForOptional(MatChipHarness.with({ text: /MCP/ }));

  public static with(options: BaseHarnessFilters): HarnessPredicate<ApiCardHarness> {
    return new HarnessPredicate(ApiCardHarness, options);
  }

  public async getTitle(): Promise<string> {
    return await this.getTitleAndVersion().then(res => res.title);
  }

  public async getVersion(): Promise<string> {
    return await this.getTitleAndVersion().then(res => res.version);
  }

  public async getDescription(): Promise<string> {
    const div = await this.locateDescription();
    return await div.text();
  }

  public async isMcpServer(): Promise<boolean> {
    const mcpServerChip = await this.locateMcpServerChip();
    return mcpServerChip !== null;
  }

  private async getTitleAndVersion(): Promise<{ title: string; version: string }> {
    return await this.locateHeaderContent()
      .then(header => header.text())
      .then(text => {
        const split = text.split('Version: ');
        return { title: split[0], version: split[1] };
      });
  }
}
