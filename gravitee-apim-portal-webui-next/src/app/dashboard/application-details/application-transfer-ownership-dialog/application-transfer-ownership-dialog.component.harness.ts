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
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApplicationTransferOwnershipDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'app-application-transfer-ownership-dialog';

  private readonly locateApplicationMemberAutocomplete = this.locatorFor(
    MatAutocompleteHarness.with({ selector: '[data-testid="transfer-ownership-application-member-input"]' }),
  );
  private readonly locateOtherUserAutocomplete = this.locatorFor(
    MatAutocompleteHarness.with({ selector: '[data-testid="transfer-ownership-other-user-input"]' }),
  );
  private readonly locateRoleSelect = this.locatorFor(
    MatSelectHarness.with({ selector: '[data-testid="transfer-ownership-role-select"]' }),
  );
  private readonly locateMethodButtons = this.locatorForAll(MatButtonHarness.with({ selector: 'app-button-toggle-option button' }));
  private readonly locateCancelButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="transfer-ownership-cancel-button"]' }),
  );
  private readonly locateSubmitButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="transfer-ownership-submit-button"]' }),
  );
  private readonly locateSearchError = this.locatorForOptional('[data-testid="transfer-ownership-user-search-error"]');
  private readonly locateApplicationMemberSearchError = this.locatorForOptional(
    '[data-testid="transfer-ownership-application-member-search-error"]',
  );
  private readonly locateSelectedTarget = this.locatorForOptional('[data-testid="transfer-ownership-selected-target"]');
  private readonly locateSubmitError = this.locatorForOptional('[data-testid="transfer-ownership-submit-error"]');
  private readonly locateRolesError = this.locatorForOptional('[data-testid="transfer-ownership-roles-error"]');

  async getMethodOptionTexts(): Promise<string[]> {
    return Promise.all((await this.locateMethodButtons()).map(button => button.getText()));
  }

  async selectMethod(text: string | RegExp): Promise<void> {
    const buttons = await this.locateMethodButtons();
    for (const button of buttons) {
      const buttonText = await button.getText();
      const matches = typeof text === 'string' ? buttonText.includes(text) : text.test(buttonText);
      if (matches) {
        await button.click();
        return;
      }
    }
    throw new Error(`Unable to find transfer method matching ${text.toString()}`);
  }

  async searchApplicationMember(query: string): Promise<void> {
    const autocomplete = await this.locateApplicationMemberAutocomplete();
    await autocomplete.enterText(query);
  }

  async getApplicationMemberOptionTexts(): Promise<string[]> {
    const autocomplete = await this.openApplicationMemberAutocomplete();
    return Promise.all((await autocomplete.getOptions()).map(option => option.getText()));
  }

  async selectApplicationMember(text: string | RegExp): Promise<void> {
    const autocomplete = await this.openApplicationMemberAutocomplete();
    return autocomplete.selectOption({ text });
  }

  async searchOtherUser(query: string): Promise<void> {
    const autocomplete = await this.locateOtherUserAutocomplete();
    await autocomplete.enterText(query);
  }

  async getOtherUserOptionTexts(): Promise<string[]> {
    const autocomplete = await this.openOtherUserAutocomplete();
    return Promise.all((await autocomplete.getOptions()).map(option => option.getText()));
  }

  async selectOtherUser(text: string | RegExp): Promise<void> {
    const autocomplete = await this.openOtherUserAutocomplete();
    return autocomplete.selectOption({ text });
  }

  async isOtherUserOptionDisabled(text: string | RegExp): Promise<boolean> {
    const autocomplete = await this.openOtherUserAutocomplete();
    const options = await autocomplete.getOptions({ text });
    return options[0]?.isDisabled() ?? false;
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

  async isSubmitDisabled(): Promise<boolean> {
    return (await this.locateSubmitButton()).isDisabled();
  }

  async clickSubmit(): Promise<void> {
    return (await this.locateSubmitButton()).click();
  }

  async clickCancel(): Promise<void> {
    return (await this.locateCancelButton()).click();
  }

  async getSearchErrorText(): Promise<string | null> {
    const element = await this.locateSearchError();
    return element ? element.text() : null;
  }

  async getApplicationMemberSearchErrorText(): Promise<string | null> {
    const element = await this.locateApplicationMemberSearchError();
    return element ? element.text() : null;
  }

  async getSelectedTargetText(): Promise<string | null> {
    const element = await this.locateSelectedTarget();
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

  private async openApplicationMemberAutocomplete(): Promise<MatAutocompleteHarness> {
    const autocomplete = await this.locateApplicationMemberAutocomplete();
    await autocomplete.blur();
    await autocomplete.focus();
    return autocomplete;
  }

  private async openOtherUserAutocomplete(): Promise<MatAutocompleteHarness> {
    const autocomplete = await this.locateOtherUserAutocomplete();
    await autocomplete.blur();
    await autocomplete.focus();
    return autocomplete;
  }
}
