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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { BannerRadioButtonHarness } from '../../components/banner-radio-button/banner-radio-button.harness';

export class PortalBannerHarness extends ComponentHarness {
  static readonly hostSelector = 'portal-banner';

  private getTitleInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName=titleText]' }));
  private getSubtitleInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName=subTitleText]' }));
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
}
