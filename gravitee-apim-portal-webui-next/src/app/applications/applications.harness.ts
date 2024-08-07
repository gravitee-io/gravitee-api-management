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
import { ComponentHarness, HarnessPredicate, BaseHarnessFilters } from '@angular/cdk/testing';
import { MatCardHarness } from '@angular/material/card/testing';

export interface ApplicationCardHarnessFilters extends BaseHarnessFilters {
  title?: string;
}

export class ApplicationCardHarness extends ComponentHarness {
  static hostSelector = 'app-application-card';

  public getCard = this.locatorFor(MatCardHarness);

  static with(options: ApplicationCardHarnessFilters = {}): HarnessPredicate<ApplicationCardHarness> {
    return new HarnessPredicate(ApplicationCardHarness, options).addOption('title', options.title, (harness, title) =>
      HarnessPredicate.stringMatches(harness.getTitle(), title),
    );
  }

  async getTitle(): Promise<string> {
    const card = await this.getCard();
    const header = await card.getHarness(MatCardHarness.with({ selector: 'h2' }));
    return header.getText();
  }
}
