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
import { ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { ApiProductGroupMembersComponentHarness } from './api-product-group-members/api-product-group-members.component.harness';

import { GioTableWrapperHarness } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export const API_PRODUCT_MANAGE_GROUPS_DIALOG_ID = 'apiProductManageGroupsDialog';

export const API_PRODUCT_ADD_MEMBER_DIALOG_ID = 'addApiProductMemberDialog';

export const API_PRODUCT_CONFIRM_REMOVE_MEMBER_DIALOG_ID = 'confirmApiProductMemberDeleteDialog';

/** Root harness predicate for the Manage groups overlay (use with `documentRootLoader().getHarness(...)`). */
export function getApiProductManageGroupsDialogPredicate(): HarnessPredicate<MatDialogHarness> {
  return MatDialogHarness.with({ selector: `#${API_PRODUCT_MANAGE_GROUPS_DIALOG_ID}` });
}

export class ApiProductMembersComponentHarness extends ComponentHarness {
  static hostSelector = 'api-product-members';

  private async getMembersNotificationToggle(): Promise<MatSlideToggleHarness> {
    return this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-testid="api_product_members_notify_toggle"]' }))();
  }

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

  async getEmptyMessageOrNull(): Promise<string | null> {
    const el = await this.locatorForOptional('[data-testid="api_product_members_table_empty"]')();
    return el ? (await el.text()).trim() : null;
  }

  async getLoadErrorMessage(): Promise<string | null> {
    const el = await this.locatorForOptional('[data-testid="api_product_members_table_error"]')();
    return el ? (await el.text()).trim() : null;
  }

  async clickRetryMembersLoad(): Promise<void> {
    const btn = await this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="api_product_members_retry_button"]' }))();
    await btn.click();
  }

  async getLoadingMembersMessage(): Promise<string | null> {
    const el = await this.locatorForOptional('[data-testid="api_product_members_table_loading"]')();
    return el ? (await el.text()).trim() : null;
  }

  async getMembersTable(): Promise<MatTableHarness> {
    return this.locatorFor(MatTableHarness)();
  }

  async getAddMemberButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="api_product_members_add_button"]' }))();
  }

  async getManageGroupsButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="api_product_members_manage_groups_button"]' }))();
  }

  async isMembersNotificationToggleChecked(): Promise<boolean> {
    return (await this.getMembersNotificationToggle()).isChecked();
  }

  async isMembersNotificationToggleDisabled(): Promise<boolean> {
    return (await this.getMembersNotificationToggle()).isDisabled();
  }

  async toggleMembersNotification(): Promise<void> {
    await (await this.getMembersNotificationToggle()).toggle();
  }

  /** Clicks **Manage groups** (throws if the control is absent, e.g. missing permission). */
  async clickManageGroups(): Promise<void> {
    const btn = await this.getManageGroupsButton();
    if (!btn) {
      throw new Error('Manage groups button not found (check gioPermission and that the environment exposes at least one group).');
    }
    await btn.click();
  }

  async getTransferOwnershipButton(): Promise<MatButtonHarness | null> {
    return this.locatorForOptional(MatButtonHarness.with({ selector: '[data-testid="api_product_members_transfer_ownership_button"]' }))();
  }

  async isTransferOwnershipVisible(): Promise<boolean> {
    return (await this.getTransferOwnershipButton()) != null;
  }

  async getDeleteMemberButton(): Promise<MatButtonHarness> {
    return this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="api_product_members_delete_button"]' }))();
  }

  async getTableWrapper(): Promise<GioTableWrapperHarness> {
    return this.locatorFor(GioTableWrapperHarness)();
  }

  async getSaveBar(): Promise<GioSaveBarHarness | null> {
    return this.locatorForOptional(GioSaveBarHarness)();
  }

  async clickSave(): Promise<void> {
    const bar = await this.getSaveBar();
    if (!bar) {
      throw new Error('gio-save-bar not found');
    }
    await bar.clickSubmit();
  }

  async clickReset(): Promise<void> {
    const bar = await this.getSaveBar();
    if (!bar) {
      throw new Error('gio-save-bar not found');
    }
    await bar.clickReset();
  }

  /** One entry per visible linked group with inherited members UI. */
  async getInheritedGroupMemberCards(): Promise<ApiProductGroupMembersComponentHarness[]> {
    return this.locatorForAll(ApiProductGroupMembersComponentHarness)();
  }
}
