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
import { MatButtonHarness } from '@angular/material/button/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class GioLicenseBannerHarness extends ComponentHarness {
  static hostSelector = 'gio-license-banner';

  protected getButton = this.locatorForOptional(MatButtonHarness);
  protected getBannerTitle = this.locatorForOptional(DivHarness.with({ selector: '.banner__wrapper__title' }));
  protected getBannerBody = this.locatorForOptional(DivHarness.with({ selector: '.banner__wrapper__body' }));

  public async buttonIsVisible(): Promise<boolean> {
    const btn = await this.getButton();
    return !!btn;
  }

  public async clickButton(): Promise<void> {
    const btn = await this.getButton();
    return await btn.click();
  }

  public async getTitle(): Promise<string> {
    const div = await this.getBannerTitle();
    return await div.getText();
  }

  public async getBody(): Promise<string> {
    const div = await this.getBannerBody();
    return await div.getText();
  }
}
