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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';

export class UserCellHarness extends ComponentHarness {
  static hostSelector = 'app-user-cell';

  private readonly locatePrimary = this.locatorForOptional('.user-cell__primary');
  private readonly locateCaption = this.locatorForOptional('.user-cell__caption');
  private readonly locateInitials = this.locatorForOptional('.user-cell__initials');
  private readonly locateAvatar = this.locatorForOptional('img.user-cell__avatar');
  private readonly locateYou = this.locatorForOptional('.user-cell__you');

  async getPrimaryText(): Promise<string | null> {
    const el = await this.locatePrimary();
    return el ? (await el.text()).trim() : null;
  }

  async getCaptionText(): Promise<string | null> {
    const el = await this.locateCaption();
    return el ? (await el.text()).trim() : null;
  }

  async hasCaption(): Promise<boolean> {
    return !!(await this.locateCaption());
  }

  async getInitialsText(): Promise<string | null> {
    const el = await this.locateInitials();
    return el ? (await el.text()).trim() : null;
  }

  async getAvatar(): Promise<TestElement | null> {
    return this.locateAvatar();
  }

  async getAvatarSrc(): Promise<string | null> {
    const img = await this.locateAvatar();
    return img ? img.getAttribute('src') : null;
  }

  async hasYouBadge(): Promise<boolean> {
    return !!(await this.locateYou());
  }

  /** Fires the image `error` event (broken URL / failed load). */
  async triggerAvatarError(): Promise<void> {
    const img = await this.locateAvatar();
    if (!img) {
      throw new Error('Expected avatar image to be present');
    }
    await img.dispatchEvent('error');
  }
}
