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

import { ComponentHarness, TestElement } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApiProductTransferOwnershipDialogHarness extends ComponentHarness {
  static hostSelector = 'api-product-transfer-ownership-dialog';

  async getUserOrGroupToggleGroup(): Promise<MatButtonToggleGroupHarness> {
    return this.locatorFor(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }))();
  }

  async getOptionalUserOrGroupToggleGroups(): Promise<MatButtonToggleGroupHarness[]> {
    return this.locatorForAll(MatButtonToggleGroupHarness.with({ selector: '[formControlName="userOrGroup"]' }))();
  }

  async getTransferButton(): Promise<MatButtonHarness> {
    return this.locatorFor(MatButtonHarness.with({ text: 'Transfer' }))();
  }

  async getUserSelect(): Promise<MatSelectHarness> {
    return this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="user"]' }))();
  }

  async getGroupSelect(): Promise<MatSelectHarness> {
    return this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="groupId"]' }))();
  }

  async getRoleSelect(): Promise<MatSelectHarness> {
    return this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="roleId"]' }))();
  }

  async getOptionalGroupsEligibilityBanner(): Promise<TestElement | null> {
    return this.locatorForOptional('[data-testid="transfer_ownership_groups_eligibility_banner"]')();
  }

  async getOptionalIrreversibleBanner(): Promise<TestElement | null> {
    return this.locatorForOptional('[data-testid="transfer_ownership_irreversible_banner"]')();
  }
}
