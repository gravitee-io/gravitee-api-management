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
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatExpansionPanelHarness } from '@angular/material/expansion/testing';

import { AgentAccessCardComponentHarness } from '../agent-access-card/agent-access-card.harness';

export class AgentSubscriptionsComponentHarness extends ComponentHarness {
  static readonly hostSelector = 'app-agent-subscriptions';

  private readonly getPanels = this.locatorForAll(MatExpansionPanelHarness);
  private readonly getNewSubscriptionButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="new-subscription-button"]' }),
  );
  private readonly getDetailsLinks = this.locatorForAll('[data-testid="subscription-details-link"]');

  async getPanelCount(): Promise<number> {
    return (await this.getPanels()).length;
  }

  async getHeaderTexts(): Promise<{ title: string | null; description: string | null }[]> {
    const panels = await this.getPanels();
    return Promise.all(
      panels.map(async panel => ({
        title: await panel.getTitle(),
        description: await panel.getDescription(),
      })),
    );
  }

  async expandAt(index: number): Promise<void> {
    await (await this.panelAt(index)).expand();
  }

  async getAccessTextAt(index: number): Promise<string> {
    return (await this.panelAt(index)).getTextContent();
  }

  async getApplicationNameAt(index: number): Promise<string | null> {
    const card = await this.cardAt(index);
    return card ? card.getApplicationNameText() : null;
  }

  async getClientIdAt(index: number): Promise<string | null> {
    const card = await this.cardAt(index);
    return card ? card.getClientIdText() : null;
  }

  async clickNewSubscription(): Promise<void> {
    await (await this.getNewSubscriptionButton()).click();
  }

  async getDetailsHrefAt(index: number): Promise<string | null> {
    const links = await this.getDetailsLinks();
    const link = links[index];
    if (!link) {
      throw new Error(`No details link at index ${index}`);
    }
    return link.getAttribute('href');
  }

  private async cardAt(index: number): Promise<AgentAccessCardComponentHarness | null> {
    return (await this.panelAt(index)).getHarnessOrNull(AgentAccessCardComponentHarness);
  }

  private async panelAt(index: number): Promise<MatExpansionPanelHarness> {
    const panels = await this.getPanels();
    const panel = panels[index];
    if (!panel) {
      throw new Error(`No subscription panel at index ${index}`);
    }
    return panel;
  }
}
