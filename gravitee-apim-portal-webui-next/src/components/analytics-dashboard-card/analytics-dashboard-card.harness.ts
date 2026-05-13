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
import { BaseHarnessFilters, ContentContainerComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';

import { OverflowLabelsHarness } from '../overflow-labels/overflow-labels.harness';

export class AnalyticsDashboardCardHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-analytics-dashboard-card';

  private readonly locateTitle = this.locatorFor('.dashboard-card__title');
  private readonly locateWidgetCount = this.locatorForOptional('[data-testid="widget-count"]');
  private readonly locateLastModified = this.locatorForOptional('[data-testid="last-modified"]');
  private readonly locateOverflowLabels = this.locatorForOptional(OverflowLabelsHarness);
  private readonly locatePinButton = this.locatorForOptional(MatButtonHarness.with({ selector: '.dashboard-card__pin-button' }));
  private readonly locatePinButtonElement = this.locatorForOptional('.dashboard-card__pin-button');

  public static with(options: BaseHarnessFilters): HarnessPredicate<AnalyticsDashboardCardHarness> {
    return new HarnessPredicate(AnalyticsDashboardCardHarness, options);
  }

  public async getTitle(): Promise<string> {
    return (await this.locateTitle()).text();
  }

  public async getWidgetCount(): Promise<string | null> {
    const element = await this.locateWidgetCount();
    return element ? element.text() : null;
  }

  public async getLastModified(): Promise<string | null> {
    const element = await this.locateLastModified();
    return element ? element.text() : null;
  }

  public async hasOverflowLabels(): Promise<boolean> {
    return (await this.locateOverflowLabels()) !== null;
  }

  public async getOverflowLabelsHarness(): Promise<OverflowLabelsHarness | null> {
    return this.locateOverflowLabels();
  }

  public async getPinButton(): Promise<MatButtonHarness | null> {
    return this.locatePinButton();
  }

  /** Native click instead of MatButtonHarness: avoids deadlock when pin starts HTTP that stays pending until HttpTestingController.flush. */
  public async clickPinButtonWithoutStabilizing(): Promise<void> {
    const pin = await this.locatePinButtonElement();
    if (!pin) throw new Error('Pin button not found on this card');
    const pinElement = TestbedHarnessEnvironment.getNativeElement(pin) as HTMLElement;
    pinElement.click();
  }

  public async click(): Promise<void> {
    const card = await this.locatorFor('.dashboard-card')();
    return card.click();
  }
}
