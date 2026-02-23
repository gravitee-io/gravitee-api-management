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

import { DivHarness } from '../../testing/div.harness';
import { BreadcrumbsComponentHarness } from '../breadcrumbs/breadcrumbs.component.harness';
import { SidenavToggleButtonComponentHarness } from '../sidenav-toggle-button/sidenav-toggle-button.component.harness';

export class SidenavLayoutComponentHarness extends ComponentHarness {
  public static hostSelector = 'app-sidenav-layout';

  private readonly getSidenavHarness = this.locatorFor(DivHarness.with({ selector: '.sidenav-layout__sidenav' }));
  private readonly getSidenavToggleButtonHarness = this.locatorFor(SidenavToggleButtonComponentHarness);
  private readonly getBreadcrumbsHarness = this.locatorForOptional(BreadcrumbsComponentHarness);

  public async getBreadcrumbs(): Promise<BreadcrumbsComponentHarness | null> {
    return this.getBreadcrumbsHarness();
  }

  public async getSidenav(): Promise<DivHarness | null> {
    return this.getSidenavHarness();
  }

  public async getSidenavToggleButton(): Promise<SidenavToggleButtonComponentHarness | null> {
    return this.getSidenavToggleButtonHarness();
  }

  public async toggleSidenav(): Promise<void> {
    const toggleButton = await this.getSidenavToggleButtonHarness();
    const button = await toggleButton.getButton();
    return button.click();
  }

  public async isSidenavCollapsed(): Promise<boolean> {
    const sidenav = await this.getSidenavHarness();
    return (await sidenav.host()).hasClass('collapsed');
  }
}
