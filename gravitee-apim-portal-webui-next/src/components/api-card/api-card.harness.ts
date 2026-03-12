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
  protected locateTitle = this.locatorFor('.next-gen-h5');
  protected locateDescription = this.locatorFor('.api-card__description');
  protected locateMcpBadge = this.locatorForOptional('app-badge');

  public static with(options: BaseHarnessFilters): HarnessPredicate<ApiCardHarness> {
    return new HarnessPredicate(ApiCardHarness, options);
  }

  public async getTitle(): Promise<string> {
    return (await this.locateTitle()).text();
  }

  public async getDescription(): Promise<string> {
    return (await this.locateDescription()).text();
  }

  public async isMcpServer(): Promise<boolean> {
    return (await this.locateMcpBadge()) !== null;
  }
}
