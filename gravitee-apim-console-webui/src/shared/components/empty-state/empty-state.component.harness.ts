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
import { MatIconHarness } from '@angular/material/icon/testing';

interface EmptyStateHarnessFilters extends BaseHarnessFilters {
  title?: string;
  message?: string;
}

export class EmptyStateComponentHarness extends ComponentHarness {
  public static hostSelector = 'empty-state';
  protected getTitleElement = this.locatorFor('[data-test-id="empty-title"]');
  protected getMessageElement = this.locatorFor('[data-test-id="empty-message"]');
  protected getIconHarness = this.locatorFor(MatIconHarness);

  static with(options: EmptyStateHarnessFilters = {}): HarnessPredicate<EmptyStateComponentHarness> {
    return new HarnessPredicate(EmptyStateComponentHarness, options)
      .addOption('title', options.title, async (harness, title) => {
        return HarnessPredicate.stringMatches(harness.getTitle(), title);
      })
      .addOption('message', options.message, async (harness, message) => {
        return HarnessPredicate.stringMatches(harness.getMessage(), message);
      });
  }

  /**
   * Gets the displayed title text.
   * @returns A promise that resolves to the title string.
   */
  public async getTitle(): Promise<string> {
    const titleEl = await this.getTitleElement();
    return titleEl.text();
  }

  /**
   * Gets the displayed message text.
   * @returns A promise that resolves to the message string.
   */
  public async getMessage(): Promise<string> {
    const messageEl = await this.getMessageElement();
    return messageEl.text();
  }
}
