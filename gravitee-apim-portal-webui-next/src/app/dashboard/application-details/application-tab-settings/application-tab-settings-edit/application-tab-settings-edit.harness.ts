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
import { ContentContainerComponentHarness, parallel, TestKey } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatChipGridHarness } from '@angular/material/chips/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

import { CopyCodeHarness } from '../../../../../components/copy-code/copy-code.harness';
import { PictureHarness } from '../../../../../components/picture/picture.harness';

export class ApplicationTabSettingsEditHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-application-tab-settings-edit';
  protected locateAppName = this.getHarness(MatInputHarness.with({ selector: '[data-testId="name"]' }));
  protected locateAppDescription = this.getHarness(MatInputHarness.with({ selector: '[data-testId="description"]' }));
  protected locateIntegrationTitle = this.locatorFor('[data-testId="integrationTitle"]');
  protected locateSimpleType = this.getHarnessOrNull(MatInputHarness.with({ selector: '[data-testId="simple-type"]' }));
  protected locateSimpleClientId = this.getHarnessOrNull(MatInputHarness.with({ selector: '[data-testId="simple-clientId"]' }));
  protected locateType = this.locatorForOptional('[data-testId="type"]');
  protected locateTypeDescription = this.locatorForOptional('[data-testId="typeDescription"]');
  protected locateRedirectUris = this.getHarnessOrNull(MatChipGridHarness.with({ selector: '[data-testId="redirectUris"]' }));
  protected locateGrantTypes = this.getHarnessOrNull(MatSelectHarness.with({ selector: '[data-testId="grantTypes"]' }));
  protected locateSaveButton = this.getHarness(MatButtonHarness.with({ selector: '[data-testId="save"]' }));
  protected locateDiscardButton = this.getHarness(MatButtonHarness.with({ selector: '[data-testId="discard"]' }));
  protected locateDeletePictureButton = this.getHarness(MatButtonHarness.with({ selector: '[data-testId="deletePicture"]' }));

  public async getDisplayedPictureSource(): Promise<string | null> {
    const pictureComponent = await this.getHarness(PictureHarness);
    return pictureComponent.getSource();
  }

  public async changePicture(fileContent: string) {
    const inputFile = await this.locatorFor('#applicationPictureFile')();
    const nativeElement = TestbedHarnessEnvironment.getNativeElement(inputFile);

    const event = new Event('change', { bubbles: true });
    Object.defineProperty(event, 'target', { value: { files: [new File([fileContent], 'New image', { type: 'image/png' })] } });
    nativeElement.dispatchEvent(event);
    await new Promise(resolve => setTimeout(resolve, 50));
  }

  public async isDeletePictureButtonDisabled(): Promise<boolean> {
    return this.locateDeletePictureButton.then(button => button.isDisabled());
  }
  public async deletePicture(): Promise<void> {
    return this.locateDeletePictureButton.then(button => button.click());
  }

  public async getName(): Promise<string> {
    return this.locateAppName.then(input => input.getValue());
  }
  public async changeName(newName: string): Promise<void> {
    return this.locateAppName.then(input => input.setValue(newName));
  }

  public async getDescription(): Promise<string> {
    return this.locateAppDescription.then(input => input.getValue());
  }
  public async changeDescription(newDescription: string): Promise<void> {
    return this.locateAppDescription.then(input => input.setValue(newDescription));
  }

  public async getIntegrationTitle(): Promise<string> {
    return this.locateIntegrationTitle().then(title => title.text());
  }

  public async getSimpleType(): Promise<string | undefined> {
    return this.locateSimpleType.then(type => type?.getValue());
  }
  public async changeSimpleType(newApplicationType: string): Promise<void> {
    return this.locateSimpleType!.then(type => type!.setValue(newApplicationType));
  }

  public async getSimpleClientId(): Promise<string | undefined> {
    return this.locateSimpleClientId.then(clientId => clientId?.getValue());
  }
  public async changeSimpleClientId(newClientId: string): Promise<void> {
    return this.locateSimpleClientId!.then(clientId => clientId!.setValue(newClientId));
  }

  public async getType(): Promise<string | undefined> {
    const testElement = await this.locateType();
    if (testElement) {
      return testElement.text().then(text => text.replace(':', ''));
    }
    return undefined;
  }
  public async getTypeDescription(): Promise<string | undefined> {
    const testElement = await this.locateTypeDescription();
    if (testElement) {
      return testElement.text();
    }
    return undefined;
  }

  public async getRedirectUris(): Promise<string[] | undefined> {
    const matChipGrid = await this.locateRedirectUris;
    const rows = await matChipGrid?.getRows();
    if (rows) {
      return await parallel(() => rows.map(row => row.getText()));
    }
    return undefined;
  }
  public async addRedirectUri(newRedirectUri: string): Promise<void> {
    const matChipGrid = await this.locateRedirectUris;
    const input = await matChipGrid?.getInput();
    if (input) {
      await input.setValue(newRedirectUri);
      await input.sendSeparatorKey(TestKey.ENTER);
    }
  }
  public async removeRedirectUri(redirectUriToRemove: string): Promise<void> {
    const matChipGrid = await this.locateRedirectUris;
    const rows = await matChipGrid?.getRows();
    const filteredRow = rows?.filter(async row => (await row.getText()) === redirectUriToRemove);
    if (filteredRow) {
      filteredRow[0].remove();
    }
  }

  public async getGrantTypes(): Promise<string[] | undefined> {
    return this.locateGrantTypes.then(matSelect => matSelect?.getValueText()).then(text => text?.split(', '));
  }
  public async checkGrantType(grantType: string | RegExp): Promise<void> {
    await this.locateGrantTypes.then(matSelect => matSelect?.clickOptions({ text: grantType }));
  }

  public async getClientId(): Promise<string | undefined> {
    return await this.getCopyCodeHarnessOrNull('clientId').then(cardCodeHarness => cardCodeHarness?.getText());
  }
  public async getHiddenClientSecret(): Promise<string | undefined> {
    return await this.getCopyCodeHarnessOrNull('clientSecret').then(cardCodeHarness => cardCodeHarness?.getText());
  }
  public async getClearClientSecret(): Promise<string | undefined> {
    const copyCodeHarnessOrNull = this.getCopyCodeHarnessOrNull('clientSecret');
    await copyCodeHarnessOrNull.then(cardCodeHarness => cardCodeHarness?.changePasswordVisibility());
    return copyCodeHarnessOrNull?.then(harness => harness?.getText());
  }

  public async isSaveButtonDisabled(): Promise<boolean> {
    return this.locateSaveButton.then(button => button.isDisabled());
  }
  public async saveApplication(): Promise<void> {
    return this.locateSaveButton.then(button => button.click());
  }

  public async isDiscardButtonDisabled(): Promise<boolean> {
    return this.locateDiscardButton.then(button => button.isDisabled());
  }
  public async discardChanges(): Promise<void> {
    return this.locateDiscardButton.then(button => button.click());
  }

  private async getCopyCodeHarnessOrNull(title: string): Promise<CopyCodeHarness | null> {
    return await this.getHarnessOrNull(CopyCodeHarness.with({ selector: `[data-testId="${title}"]` }));
  }
}
