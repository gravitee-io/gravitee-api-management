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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatTooltipHarness } from '@angular/material/tooltip/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { GioTableWrapperHarness } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export interface PortalAuthenticationRow {
  id: string;
  name: string;
  description: string;
  activated: boolean;
}

export class PortalAuthenticationHarness extends ComponentHarness {
  static readonly hostSelector = 'portal-authentication';

  private readonly forceLoginToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-testid="force-login-toggle"]' }));
  private readonly localLoginToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[data-testid="local-login-toggle"]' }));
  private readonly table = this.locatorFor(MatTableHarness);
  private readonly tableWrapper = this.locatorFor(GioTableWrapperHarness);
  private readonly saveBar = this.locatorFor(GioSaveBarHarness);
  private readonly emptyMessage = this.locatorFor('[data-testid="identity-providers-empty"]');

  async getForceLoginToggle(): Promise<MatSlideToggleHarness> {
    return this.forceLoginToggle();
  }

  async getLocalLoginToggle(): Promise<MatSlideToggleHarness> {
    return this.localLoginToggle();
  }

  async submit(): Promise<void> {
    await (await this.saveBar()).clickSubmit();
  }

  async search(term: string): Promise<void> {
    await (await this.tableWrapper()).setSearchValue(term);
  }

  async getRows(): Promise<PortalAuthenticationRow[]> {
    const rows = await (await this.table()).getRows();
    return Promise.all(
      rows.map(async row => {
        const cells = await row.getCellTextByColumnName();
        const activation = await row.getCells({ columnName: 'actions' }).then(([cell]) => cell.host());
        return {
          id: cells.id,
          name: cells.name,
          description: cells.description,
          activated: (await activation.getAttribute('data-activated')) === 'true',
        };
      }),
    );
  }

  async getEmptyMessage(): Promise<string> {
    return (await this.emptyMessage()).text();
  }

  async clickActivation(identityProviderId: string): Promise<void> {
    await (await this.activationButton(identityProviderId)).click();
  }

  async isActivationDisabled(identityProviderId: string): Promise<boolean> {
    return (await this.activationButton(identityProviderId)).isDisabled();
  }

  async hasActivationAction(identityProviderId: string): Promise<boolean> {
    const button = await this.locatorForOptional(
      MatButtonHarness.with({ selector: `[data-testid="idp-activation-${identityProviderId}"]` }),
    )();
    return button !== null;
  }

  async getActivationTooltip(identityProviderId: string): Promise<string> {
    const tooltip = await this.locatorFor(
      MatTooltipHarness.with({ selector: `[data-testid="idp-activation-tooltip-${identityProviderId}"]` }),
    )();
    await tooltip.show();
    return tooltip.getTooltipText();
  }

  private activationButton(identityProviderId: string): Promise<MatButtonHarness> {
    return this.locatorFor(MatButtonHarness.with({ selector: `[data-testid="idp-activation-${identityProviderId}"]` }))();
  }
}
