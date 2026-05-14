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
import { MatChipHarness } from '@angular/material/chips/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApplicationMembersAddDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'app-application-members-add-dialog';

  private readonly locateUserAutocomplete = this.locatorFor(
    MatAutocompleteHarness.with({ selector: '[data-testid="add-members-search-input"]' }),
  );
  private readonly locateRoleSelect = this.locatorFor(MatSelectHarness.with({ selector: '[data-testid="add-members-role-select"]' }));
  private readonly locateSubmitButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="add-members-submit-button"]' }));
  private readonly locateCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="add-members-cancel-button"]' }));
  private readonly locateSearchError = this.locatorForOptional('[data-testid="add-members-search-error"]');
  private readonly locateSubmitError = this.locatorForOptional('[data-testid="add-members-submit-error"]');
  private readonly locateUserErrors = this.locatorForAll('[data-testid="add-members-user-error"]');
  private readonly locateSelectedUserChips = this.locatorForAll(
    MatChipHarness.with({ selector: '[data-testid="add-members-selected-user-chip"]' }),
  );

  async getSearchNoMatchText(): Promise<string | null> {
    const autocomplete = await this.openUserAutocomplete();
    const options = await autocomplete.getOptions({ text: /No users found/ });
    return options[0]?.getText() ?? null;
  }

  async getSearchErrorText(): Promise<string | null> {
    const element = await this.locateSearchError();
    return element ? element.text() : null;
  }

  async getSubmitErrorText(): Promise<string | null> {
    const element = await this.locateSubmitError();
    return element ? element.text() : null;
  }

  async getUserErrorTexts(): Promise<string[]> {
    return Promise.all((await this.locateUserErrors()).map(element => element.text()));
  }

  async getUserOptionTexts(): Promise<string[]> {
    const autocomplete = await this.openUserAutocomplete();
    return Promise.all((await autocomplete.getOptions()).map(option => option.getText()));
  }

  async getSelectedUserChipTexts(): Promise<string[]> {
    return Promise.all((await this.locateSelectedUserChips()).map(chip => chip.getText()));
  }

  async removeSelectedUserChip(text: string | RegExp): Promise<void> {
    const chips = await this.locateSelectedUserChips();
    for (const chip of chips) {
      const chipText = await chip.getText();
      const matches = typeof text === 'string' ? chipText.includes(text) : text.test(chipText);
      if (matches) {
        await chip.remove();
        return;
      }
    }
    throw new Error(`Unable to find selected user chip matching ${text.toString()}`);
  }

  async selectUser(text: string | RegExp): Promise<void> {
    const autocomplete = await this.openUserAutocomplete();
    return autocomplete.selectOption({ text });
  }

  async isUserOptionDisabled(text: string | RegExp): Promise<boolean> {
    const autocomplete = await this.openUserAutocomplete();
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

  private async openUserAutocomplete(): Promise<MatAutocompleteHarness> {
    const autocomplete = await this.locateUserAutocomplete();
    await autocomplete.blur();
    await autocomplete.focus();
    return autocomplete;
  }
}
