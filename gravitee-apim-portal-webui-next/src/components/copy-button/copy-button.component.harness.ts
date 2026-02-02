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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class CopyButtonHarness extends ComponentHarness {
  static readonly hostSelector = 'app-copy-button';

  private readonly buttonHarness = this.locatorFor(MatButtonHarness);
  private readonly iconHarness = this.locatorFor('.material-icons');

  async click(): Promise<void> {
    const btn = await this.buttonHarness();
    await btn.click();
  }

  async getIconText(): Promise<string> {
    const icon = await this.iconHarness();
    return (await icon.text()).trim();
  }

  async getAriaLabel(): Promise<string | null> {
    const button = await this.buttonHarness();// 2. Get the actual DOM element (TestElement) of that button
    const buttonHost = await button.host();
    return buttonHost.getAttribute('aria-label');
  }
}
