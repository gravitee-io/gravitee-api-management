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
import { MatInputHarness } from '@angular/material/input/testing';
import { GioFormSelectionInlineCardHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { BannerRadioButtonHarness } from '../../components/banner-radio-button/banner-radio-button.harness';

export class PortalBannerHarness extends ComponentHarness {
  static readonly hostSelector = 'portal-banner';

  private getTitleInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName=titleText]' }));
  private getSubtitleInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName=subTitleText]' }));
  private locatePrimaryButtonTextInput = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Set primary button text"]' }));
  private locateSecondaryButtonTextInput = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Set secondary button text"]' }));
  private locatePrimaryButtonTargetInput = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Set primary button url"]' }));
  private locateSecondaryButtonTargetInput = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Set secondary button url"]' }));
  private locateAllToggles = this.locatorForAll(MatSlideToggleHarness);
  private locatePrimaryButtonVisibilityOptions = this.locatorForAll(
    GioFormSelectionInlineCardHarness.with({ ancestor: '[data-testid="primary-button-visibility"]' }),
  );
  private locateSecondaryButtonVisibilityOptions = this.locatorForAll(
    GioFormSelectionInlineCardHarness.with({ ancestor: '[data-testid="secondary-button-visibility"]' }),
  );
  private getSaveBar = this.locatorFor(GioSaveBarHarness);
  private locateBannerRadio = (title: string) => this.locatorFor(BannerRadioButtonHarness.with({ title }))();

  public async setTitle(title: string) {
    return this.getTitleInput().then((input) => input.setValue(title));
  }

  public async getTitle() {
    return this.getTitleInput().then((input) => input.getValue());
  }

  public async setSubtitle(subtitle: string) {
    return this.getSubtitleInput().then((input) => input.setValue(subtitle));
  }

  public async getSubtitle() {
    return this.getSubtitleInput().then((input) => input.getValue());
  }

  public async enableBanner(): Promise<void> {
    return await this.locateBannerRadio('Featured banner')
      .then((bannerRadio) => bannerRadio.getRadioButton())
      .then((radio) => radio.check());
  }

  public async disableBanner(): Promise<void> {
    return await this.locateBannerRadio('None')
      .then((bannerRadio) => bannerRadio.getRadioButton())
      .then((radio) => radio.check());
  }

  public async submit() {
    return this.getSaveBar().then((saveBar) => saveBar.clickSubmit());
  }

  public reset() {
    return this.getSaveBar().then((saveBar) => saveBar.clickReset());
  }

  /**
   * Primary button
   */

  public async getPrimaryButtonTextInput(): Promise<MatInputHarness> {
    return await this.locatePrimaryButtonTextInput();
  }

  public async getPrimaryButtonTargetInput(): Promise<MatInputHarness> {
    return await this.locatePrimaryButtonTargetInput();
  }

  public async getPrimaryButtonEnableToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateAllToggles().then((toggles) => toggles[0]);
  }

  public async getPrimaryButtonVisibilityValue(): Promise<string> {
    return await this.getVisibilityValue(await this.locatePrimaryButtonVisibilityOptions());
  }

  public async setPrimaryButtonVisibility(visibility: 'PUBLIC' | 'PRIVATE'): Promise<void> {
    return await this.selectVisibilityOption(visibility, await this.locatePrimaryButtonVisibilityOptions());
  }

  /**
   * Secondary button
   */

  public async getSecondaryButtonTextInput(): Promise<MatInputHarness> {
    return await this.locateSecondaryButtonTextInput();
  }

  public async getSecondaryButtonTargetInput(): Promise<MatInputHarness> {
    return await this.locateSecondaryButtonTargetInput();
  }

  public async getSecondaryButtonEnableToggle(): Promise<MatSlideToggleHarness> {
    return await this.locateAllToggles().then((toggles) => toggles[1]);
  }

  public async getSecondaryButtonVisibilityValue(): Promise<string> {
    return await this.getVisibilityValue(await this.locateSecondaryButtonVisibilityOptions());
  }

  public async setSecondaryButtonVisibility(visibility: 'PUBLIC' | 'PRIVATE'): Promise<void> {
    return await this.selectVisibilityOption(visibility, await this.locateSecondaryButtonVisibilityOptions());
  }

  private async getVisibilityValue(inlineHarnesses: GioFormSelectionInlineCardHarness[]): Promise<string> {
    const publicOption = inlineHarnesses[0];
    const privateOption = inlineHarnesses[1];
    if (await publicOption.isSelected()) {
      return await publicOption.getValue();
    }
    if (await privateOption.isSelected()) {
      return await privateOption.getValue();
    }
    return new Promise(null);
  }

  private async selectVisibilityOption(
    visibilityOptionValue: 'PUBLIC' | 'PRIVATE',
    inlineHarnesses: GioFormSelectionInlineCardHarness[],
  ): Promise<void> {
    const publicOption = inlineHarnesses[0];
    const privateOption = inlineHarnesses[1];
    if (visibilityOptionValue === 'PUBLIC') {
      return await publicOption.host().then((opt) => opt.click());
    }
    return await privateOption.host().then((opt) => opt.click());
  }
}
