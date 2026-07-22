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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatTooltipHarness } from '@angular/material/tooltip/testing';

export class FailedHealthChecksHarness extends ComponentHarness {
  static hostSelector = 'failed-health-checks';

  private viewDetailsButtons = this.locatorForAll(MatButtonHarness.with({ selector: '[data-testid="view-details-button"]' }));

  async getTitle(): Promise<string> {
    const el = await this.locatorFor(MatCardHarness)();
    return el.getTitleText();
  }

  async getSubtitle(): Promise<string> {
    const el = await this.locatorFor(MatCardHarness)();
    return el.getSubtitleText();
  }

  public tableHarness = this.locatorForOptional(MatTableHarness);

  async getViewDetailsButtonCount(): Promise<number> {
    return (await this.viewDetailsButtons()).length;
  }

  async isViewDetailsButtonDisabled(rowIndex: number): Promise<boolean> {
    const buttons = await this.viewDetailsButtons();
    return buttons[rowIndex].isDisabled();
  }

  async clickViewDetails(rowIndex: number): Promise<void> {
    const buttons = await this.viewDetailsButtons();
    return buttons[rowIndex].click();
  }

  /** Scoped by selector: the paginator buttons carry tooltips too, so a bare lookup would not be row-aligned. */
  async getViewDetailsTooltip(rowIndex: number): Promise<string> {
    const tooltips = await this.locatorForAll(MatTooltipHarness.with({ selector: '[data-testid="view-details-button"]' }))();
    await tooltips[rowIndex].show();

    return tooltips[rowIndex].getTooltipText();
  }

  /**
   * A natively disabled button suppresses pointer events in a real browser, which would silently kill its
   * tooltip. jsdom does not reproduce that, so the guarantee is asserted structurally instead.
   */
  async getViewDetailsDisabledAttributes(rowIndex: number): Promise<{ nativeDisabled: string | null; ariaDisabled: string | null }> {
    const host = await (await this.viewDetailsButtons())[rowIndex].host();
    return {
      nativeDisabled: await host.getAttribute('disabled'),
      ariaDisabled: await host.getAttribute('aria-disabled'),
    };
  }
}
