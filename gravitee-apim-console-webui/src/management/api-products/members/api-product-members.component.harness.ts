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
import { MatTableHarness } from '@angular/material/table/testing';

import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export class ApiProductMembersComponentHarness extends ComponentHarness {
  static hostSelector = 'api-product-members';

  async getTitle(): Promise<string> {
    const el = await this.locatorFor('[data-testid="api_product_members_title"]')();
    return (await el.text()).trim();
  }

  async getDescription(): Promise<string> {
    const el = await this.locatorFor('[data-testid="api_product_members_description"]')();
    return el.text();
  }

  async getEmptyMessage(): Promise<string> {
    const el = await this.locatorFor('[data-testid="api_product_members_table_empty"]')();
    return (await el.text()).trim();
  }

  async getMembersTable(): Promise<MatTableHarness> {
    return this.locatorFor(MatTableHarness)();
  }

  async getAddMemberButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="api_product_members_add_button"]' }))();
  }

  async getDeleteMemberButton(): Promise<MatButtonHarness> {
    return this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="api_product_members_delete_button"]' }))();
  }

  async getTableWrapper(): Promise<GioTableWrapperHarness> {
    return this.locatorFor(GioTableWrapperHarness)();
  }
}
