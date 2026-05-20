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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApplicationMemberEditDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'app-application-member-edit-dialog';

  private readonly locateRoleSelect = this.locatorFor(MatSelectHarness.with({ selector: '[data-testid="edit-member-role-select"]' }));
  private readonly locateCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="edit-member-cancel-button"]' }));
  private readonly locateSaveButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="edit-member-save-button"]' }));
  private readonly locateRolesError = this.locatorForOptional('[data-testid="edit-member-roles-error"]');
  private readonly locateSubmitError = this.locatorForOptional('[data-testid="edit-member-submit-error"]');

  async getRoleValueText(): Promise<string> {
    return (await this.locateRoleSelect()).getValueText();
  }

  async getRoleOptionTexts(): Promise<string[]> {
    const select = await this.locateRoleSelect();
    await select.open();
    return Promise.all((await select.getOptions()).map(option => option.getText()));
  }

  async selectRole(role: string): Promise<void> {
    const select = await this.locateRoleSelect();
    await select.open();
    await select.clickOptions({ text: role });
  }

  async clickCancel(): Promise<void> {
    return (await this.locateCancelButton()).click();
  }

  async clickSave(): Promise<void> {
    return (await this.locateSaveButton()).click();
  }

  async isCancelDisabled(): Promise<boolean> {
    return (await this.locateCancelButton()).isDisabled();
  }

  async isSaveDisabled(): Promise<boolean> {
    return (await this.locateSaveButton()).isDisabled();
  }

  async getRolesErrorText(): Promise<string | null> {
    const error = await this.locateRolesError();
    return error ? error.text() : null;
  }

  async getSubmitErrorText(): Promise<string | null> {
    const error = await this.locateSubmitError();
    return error ? error.text() : null;
  }
}
