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
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ClusterTransferOwnershipDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'cluster-transfer-ownership-dialog';

  private getToggleGroup = () => this.locatorFor(MatButtonToggleGroupHarness)();
  private getEntityMemberSelect = () =>
    this.locatorForOptional(MatSelectHarness.with({ selector: 'mat-select[formcontrolname="entityMember"]' }))();
  private getRoleSelect = () => this.locatorForOptional(MatSelectHarness.with({ selector: 'mat-select[formcontrolname="roleId"]' }))();
  private getSubmitButton = () => this.locatorForOptional(MatButtonHarness.with({ text: /Transfer/i }))();

  public async selectTransferMode(mode: 'ENTITY_MEMBER' | 'OTHER_USER'): Promise<void> {
    const group = await this.getToggleGroup();
    const toggles = await group.getToggles({ text: mode === 'ENTITY_MEMBER' ? /Cluster member/i : /Other user/i });
    if (toggles.length === 0) throw new Error(`No toggle matching: ${mode}`);
    await toggles[0].toggle();
  }

  public async selectEntityMemberByText(optionText: string): Promise<void> {
    const select = await this.getEntityMemberSelect();
    if (!select) throw new Error('Entity member select not available');
    await select.open();
    const options = await select.getOptions({ text: optionText });
    if (options.length === 0) throw new Error(`No entity member option matching: ${optionText}`);
    await options[0].click();
  }

  public async selectRoleByText(roleText: string): Promise<void> {
    const select = await this.getRoleSelect();
    if (!select) throw new Error('Role select not available');
    await select.open();
    const options = await select.getOptions({ text: roleText });
    if (options.length === 0) throw new Error(`No role option matching: ${roleText}`);
    await options[0].click();
  }

  public async isSubmitEnabled(): Promise<boolean> {
    const btn = await this.getSubmitButton();
    if (!btn) return false;
    return btn.isDisabled().then(d => !d);
  }

  public async submit(): Promise<void> {
    const btn = await this.getSubmitButton();
    if (!btn) throw new Error('Submit button not found');
    await btn.click();
  }
}
