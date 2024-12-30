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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatCardHarness } from '@angular/material/card/testing';

export type AvailabilityPerFieldHarnessFilters = BaseHarnessFilters & {
  title?: 'Availability Per-Endpoint' | 'Availability Per-Gateway';
};

export class AvailabilityPerFieldHarness extends ComponentHarness {
  static hostSelector = 'availability-per-field';

  static with(options: AvailabilityPerFieldHarnessFilters = {}): HarnessPredicate<AvailabilityPerFieldHarness> {
    return new HarnessPredicate(AvailabilityPerFieldHarness, options).addOption('title', options.title, (harness, field) =>
      HarnessPredicate.stringMatches(harness.getTitle(), field),
    );
  }

  async getTitle(): Promise<string> {
    const el = await this.locatorFor(MatCardHarness)();
    return el.getTitleText();
  }

  async getSubtitle(): Promise<string> {
    const el = await this.locatorFor(MatCardHarness)();
    return el.getSubtitleText();
  }

  public tableHarness = this.locatorForOptional(MatTableHarness);
}
