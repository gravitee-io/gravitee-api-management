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
import { MatTableHarness } from '@angular/material/table/testing';

import { GioTableWrapperHarness } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

/** Inherited group members card under the direct members table. */
export class ApiProductGroupMembersComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'api-product-group-members';

  /** Primary heading in the card (inherited members title or permission-denied title). */
  async getCardHeading(): Promise<string> {
    const el = await this.locatorFor('mat-card-content h3')();
    return (await el.text()).trim();
  }

  async getPermissionDeniedMessage(): Promise<string | null> {
    const el = await this.locatorForOptional('#cannot-view-members')();
    return el ? (await el.text()).trim() : null;
  }

  async getInheritedMembersTableWrapper(): Promise<GioTableWrapperHarness | null> {
    return this.locatorForOptional(GioTableWrapperHarness)();
  }

  async getInheritedMembersTable(): Promise<MatTableHarness | null> {
    return this.locatorForOptional(MatTableHarness)();
  }
}
