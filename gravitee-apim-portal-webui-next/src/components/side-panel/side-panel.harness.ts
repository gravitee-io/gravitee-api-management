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
import { ComponentHarness } from '@angular/cdk/testing';

export class SidePanelComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-side-panel';

  private readonly getBackdrop = this.locatorFor('.side-panel__backdrop');
  private readonly getPanel = this.locatorFor('.side-panel__panel');
  private readonly getCloseButton = this.locatorFor('.side-panel__panel__close-button');

  async getPanelTestId(): Promise<string | null> {
    return (await this.getPanel()).getAttribute('data-testid');
  }

  async getPanelAriaLabel(): Promise<string | null> {
    return (await this.getPanel()).getAttribute('aria-label');
  }

  async clickBackdrop(): Promise<void> {
    await (await this.getBackdrop()).click();
  }

  async clickClose(): Promise<void> {
    await (await this.getCloseButton()).click();
  }

  async pressEscape(): Promise<void> {
    await (await this.getPanel()).dispatchEvent('keydown', { key: 'Escape' });
  }
}
