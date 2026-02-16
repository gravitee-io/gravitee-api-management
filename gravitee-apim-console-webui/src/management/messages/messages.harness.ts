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
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { GioFormHeadersHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class MessagesHarness extends ComponentHarness {
  static hostSelector = 'messages';

  protected getChannelSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName=channel]' }));
  protected getRecipientsSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName=recipients]' }));
  protected getTitleInput = this.locatorForAll(MatInputHarness.with({ selector: '[formControlName=title]' }));
  protected getTextInput = this.locatorForAll(MatInputHarness.with({ selector: '[formControlName=text]' }));
  protected getUrlInput = this.locatorForAll(MatInputHarness.with({ selector: '[formControlName=url]' }));
  protected getFormHeadersControl = this.locatorForAll(GioFormHeadersHarness);
  protected getUseSystemProxyToggle = this.locatorFor(MatSlideToggleHarness);
  protected getSubmitButton = this.locatorFor(MatButtonHarness.with({ selector: '[type=submit]' }));

  public getAvailableChannel(): Promise<string[]> {
    return this.getChannelSelect().then(async select => {
      await select.open();

      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public getSelectedChannel(): Promise<string> {
    return this.getChannelSelect().then(async select => {
      return select.getValueText();
    });
  }

  public selectChannel(channelToSelect: string) {
    return this.getChannelSelect().then(async select => {
      await select.open();
      return select.clickOptions({ text: channelToSelect });
    });
  }

  public getAvailableRecipients(): Promise<string[]> {
    return this.getRecipientsSelect().then(async select => {
      await select.open();

      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public selectRecipients(recipientsToSelect: string[]): Promise<void[]> {
    return this.getRecipientsSelect().then(async select => {
      await select.open();
      return Promise.all(
        recipientsToSelect.map(async r => {
          return select.clickOptions({ text: r });
        }),
      );
    });
  }

  async isTitleControlDisplayed() {
    return this.getTitleInput().then(el => el.length > 0);
  }

  async isUrlControlDisplayed() {
    return this.getUrlInput().then(el => el.length > 0);
  }

  async isFormHeadersControlDisplayed() {
    return this.getFormHeadersControl().then(el => el.length > 0);
  }

  async setTitle(title: string): Promise<void> {
    return this.getTitleInput().then(input => {
      expect(input.length).toEqual(1);
      return input[0].setValue(title);
    });
  }

  async setText(text: string): Promise<void> {
    return this.getTextInput().then(inputs => {
      expect(inputs.length).toEqual(1);
      return inputs[0].setValue(text);
    });
  }

  async isSubmitButtonDisabled(): Promise<boolean> {
    return this.getSubmitButton().then(button => button.isDisabled());
  }

  async clickOnSubmitButton(): Promise<void> {
    return this.getSubmitButton().then(async button => {
      expect(await button.isDisabled()).toBeFalsy();
      return button.click();
    });
  }

  async setUrl(url: string) {
    return this.getUrlInput().then(inputs => {
      expect(inputs.length).toEqual(1);
      return inputs[0].setValue(url);
    });
  }

  async setHeaders(key: string, value: string) {
    return this.getFormHeadersControl().then(ctrl => {
      expect(ctrl.length).toEqual(1);
      ctrl[0].addHeader({ key, value });
    });
  }

  async toggleUseSystemProxy() {
    return this.getUseSystemProxyToggle().then(toggle => toggle.toggle());
  }
}
