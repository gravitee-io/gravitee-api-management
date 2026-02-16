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
import { MatMenuHarness } from '@angular/material/menu/testing';

import { PageType } from '../../../../../entities/page';

export class ApiDocumentationV4EmptyStateHarness extends ComponentHarness {
  static hostSelector = 'api-documentation-empty-state';
  private addNewPageButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Add new page' }));
  private menuLocator = this.locatorFor(MatMenuHarness);

  async clickAddNewPage(pageType: PageType) {
    return this.addNewPageButtonLocator()
      .then(btn => btn.click())
      .then(() => this.menuLocator())
      .then(menu => menu.clickItem({ text: new RegExp(pageType, 'i') }));
  }
}
