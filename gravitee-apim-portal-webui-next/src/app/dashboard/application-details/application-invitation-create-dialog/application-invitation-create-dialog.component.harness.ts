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
import { ComponentHarness, TestKey } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatChipHarness } from '@angular/material/chips/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApplicationInvitationCreateDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-application-invitation-create-dialog';

  private readonly locateEmailInput = this.locatorFor('[data-testid="create-invitation-email-input"]');
  private readonly locateRoleSelect = this.locatorFor(MatSelectHarness.with({ selector: '[data-testid="create-invitation-role-select"]' }));
  private readonly locateSubmitButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="create-invitation-submit-button"]' }),
  );
  private readonly locateCancelButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="create-invitation-cancel-button"]' }),
  );
  private readonly locateNotifyCheckbox = this.locatorFor(
    MatCheckboxHarness.with({ selector: '[data-testid="create-invitation-notify-checkbox"]' }),
  );
  private readonly locateEmailError = this.locatorForOptional('[data-testid="create-invitation-email-error"]');
  private readonly locateSubmitError = this.locatorForOptional('[data-testid="create-invitation-submit-error"]');
  private readonly locateRolesError = this.locatorForOptional('[data-testid="create-invitation-roles-error"]');
  private readonly locateSelectedEmailChips = this.locatorForAll(
    MatChipHarness.with({ selector: '[data-testid="create-invitation-email-chip"]' }),
  );

  async enterEmail(email: string): Promise<void> {
    const input = await this.locateEmailInput();
    await input.focus();
    return input.sendKeys(email, TestKey.ENTER);
  }

  async getSelectedEmailChipTexts(): Promise<string[]> {
    return Promise.all((await this.locateSelectedEmailChips()).map(chip => chip.getText()));
  }

  async removeSelectedEmailChip(text: string | RegExp): Promise<void> {
    const chips = await this.locateSelectedEmailChips();
    for (const chip of chips) {
      const chipText = await chip.getText();
      const matches = typeof text === 'string' ? chipText.includes(text) : text.test(chipText);
      if (matches) {
        await chip.remove();
        return;
      }
    }
    throw new Error(`Unable to find selected email chip matching ${text.toString()}`);
  }

  async selectRole(role: string): Promise<void> {
    const select = await this.locateRoleSelect();
    await select.open();
    return select.clickOptions({ text: role });
  }

  async getRoleOptionTexts(): Promise<string[]> {
    const select = await this.locateRoleSelect();
    await select.open();
    return Promise.all((await select.getOptions()).map(option => option.getText()));
  }

  async getRoleValueText(): Promise<string> {
    return (await this.locateRoleSelect()).getValueText();
  }

  async clickSubmit(): Promise<void> {
    return (await this.locateSubmitButton()).click();
  }

  async getSubmitButtonText(): Promise<string> {
    return (await this.locateSubmitButton()).getText();
  }

  async isSubmitDisabled(): Promise<boolean> {
    return (await this.locateSubmitButton()).isDisabled();
  }

  async clickCancel(): Promise<void> {
    return (await this.locateCancelButton()).click();
  }

  async isNotifyChecked(): Promise<boolean> {
    return (await this.locateNotifyCheckbox()).isChecked();
  }

  async isNotifyDisabled(): Promise<boolean> {
    return (await this.locateNotifyCheckbox()).isDisabled();
  }

  async toggleNotify(): Promise<void> {
    return (await this.locateNotifyCheckbox()).toggle();
  }

  async getEmailErrorText(): Promise<string | null> {
    const element = await this.locateEmailError();
    return element ? element.text() : null;
  }

  async getSubmitErrorText(): Promise<string | null> {
    const element = await this.locateSubmitError();
    return element ? element.text() : null;
  }

  async getRolesErrorText(): Promise<string | null> {
    const element = await this.locateRolesError();
    return element ? element.text() : null;
  }
}
