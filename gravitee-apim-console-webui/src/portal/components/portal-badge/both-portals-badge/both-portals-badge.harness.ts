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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';

export class BothPortalsBadgeHarness extends ComponentHarness {
  static hostSelector = 'both-portals-badge';

  private badgeWarningLocator = this.locatorFor('[data-testid="badge-warning"]');
  private badgeIconLocator = this.locatorFor('[data-testid="badge-icon"]');

  async getBadgeWarningText(): Promise<string> {
    const badge = await this.badgeWarningLocator();
    return badge.text();
  }

  async getBadgeIconName(): Promise<TestElement> {
    return await this.badgeIconLocator();
  }
}
