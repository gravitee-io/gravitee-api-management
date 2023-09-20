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
import { MatIconHarness } from '@angular/material/icon/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatTabHarness } from '@angular/material/tabs/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiRuntimeLogsMessagesHarness extends ComponentHarness {
  static hostSelector = 'runtime-logs-messages';

  private tabBodySelector = this.locatorFor(DivHarness.with({ selector: '.log__data__tabs__body' }));
  private load5MoreSelector = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Load 5 more"]' }));
  private messagesSelector = this.locatorForAll(DivHarness.with({ selector: '.log__header__title' }));

  public connectorIcon = this.locatorFor(MatIconHarness.with({ selector: '[data-testId=connector-icon]' }));

  public clickOnTab = async (tabLabel: string) => {
    return await this.locatorFor(MatTabHarness.with({ label: tabLabel }))().then((tab) => tab.select());
  };

  public getTabBody = async () => {
    return await this.tabBodySelector().then((tabBody) => tabBody.getText());
  };

  public load5More = async () => {
    return await this.load5MoreSelector().then((tab) => tab.click());
  };

  public getMessages = () => {
    return this.messagesSelector();
  };
}
