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
import { MatMenuHarness, MatMenuItemHarness } from '@angular/material/menu/testing';

export class PortalNavigationItemsHarness extends ComponentHarness {
  static hostSelector = 'portal-navigation-items';

  private getAddButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Add new section"]' }));
  private getMenu = this.locatorFor(MatMenuHarness);

  async getAddButtonHarness(): Promise<MatButtonHarness> {
    return this.getAddButton();
  }

  async clickAddButton(): Promise<void> {
    const button = await this.getAddButton();
    return button.click();
  }

  async getPageMenuItem(): Promise<MatMenuItemHarness> {
    const menu = await this.getMenu();
    return menu.getHarness(MatMenuItemHarness.with({ text: /Add Page/i }));
  }

  async clickPageMenuItem(): Promise<void> {
    const menuItem = await this.getPageMenuItem();
    return menuItem.click();
  }
}
