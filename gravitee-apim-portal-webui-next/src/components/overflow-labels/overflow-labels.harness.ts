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
import { ContentContainerComponentHarness } from '@angular/cdk/testing';

export class OverflowLabelsHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-overflow-labels';

  private readonly locateVisibleBadges = this.locatorForAll('[data-testid="visible-badge"]');
  private readonly locateOverflowCounter = this.locatorForOptional('[data-testid="overflow-counter"]');

  public async getVisibleBadgeCount(): Promise<number> {
    return (await this.locateVisibleBadges()).length;
  }

  public async getOverflowCounterText(): Promise<string | null> {
    const element = await this.locateOverflowCounter();
    return element ? element.text() : null;
  }
}
