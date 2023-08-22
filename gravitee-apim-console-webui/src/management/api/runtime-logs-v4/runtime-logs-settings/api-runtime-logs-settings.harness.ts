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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';

export class ApiRuntimeLogsSettingsHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-settings';

  private getEnableToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
  public getLogsBanner = this.locatorFor(DivHarness.with({ text: /Logging smartly/ }));

  public areLogsEnabled = async (): Promise<boolean> => {
    return await this.getEnableToggle().then((toggles) => toggles.isChecked());
  };
}
