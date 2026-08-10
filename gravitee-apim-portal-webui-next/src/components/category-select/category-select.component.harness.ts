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
import { MatMenuHarness } from '@angular/material/menu/testing';

export class CategorySelectHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-category-select';

  protected locateMenu = this.locatorFor(MatMenuHarness);
  protected locateTriggerValue = this.locatorFor('.category-select__value');

  async getSelectedText(): Promise<string> {
    return (await this.locateTriggerValue()).text();
  }

  async selectCategory(text: string): Promise<void> {
    const menu = await this.locateMenu();
    await menu.open();
    const [item] = await menu.getItems({ text });
    await item.click();
  }
}
