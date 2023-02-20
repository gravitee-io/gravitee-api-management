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

import { GioFormListenersContextPathHarness } from '../../../../../../components/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.harness';
import { GioFormListenersVirtualHostHarness } from '../../../../../../components/gio-form-listeners/gio-form-listeners-virtual-host/gio-form-listeners-virtual-host.harness';

export class Step2Entrypoints2ConfigHarness extends ComponentHarness {
  static hostSelector = 'step-2-entrypoints-2-config';

  protected getPreviousButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#previous',
    }),
  );

  protected getValidateButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#validate',
    }),
  );

  protected getSwitchListenersTypeButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#switchListenerType',
    }),
  );

  async clickPrevious(): Promise<void> {
    return this.getPreviousButton().then((elt) => elt.click());
  }

  async clickValidate() {
    return this.getValidateButton().then((elt) => elt.click());
  }

  async hasValidationDisabled(): Promise<boolean> {
    return this.getValidateButton().then((elt) => elt.isDisabled());
  }

  async clickListenerType() {
    return this.getSwitchListenersTypeButton().then((elt) => elt.click());
  }

  async fillPaths(...paths: string[]) {
    const formPaths = await this.locatorFor(GioFormListenersContextPathHarness.with())();

    // Add path on last path row
    for (const path of paths) {
      const emptyLastContextPathRow = await formPaths.getLastListenerRow();
      await emptyLastContextPathRow.pathInput.setValue(path);
    }
  }

  async fillPathsAndValidate(...paths: string[]) {
    await this.fillPaths(...paths);
    await this.clickValidate();
  }

  async fillVirtualHostsAndValidate(...virtualHosts: { path: string; host: string }[]) {
    const formVirtualHosts = await this.locatorFor(GioFormListenersVirtualHostHarness.with())();

    for (const virtualHost of virtualHosts) {
      const lastListenerRow = await formVirtualHosts.getLastListenerRow();
      await lastListenerRow.hostInput.setValue(virtualHost.host);
      await lastListenerRow.pathInput.setValue(virtualHost.path);
    }

    await this.clickValidate();
  }
}
