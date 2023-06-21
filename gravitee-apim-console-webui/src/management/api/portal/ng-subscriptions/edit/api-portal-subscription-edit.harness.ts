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
import { MatDialogHarness } from '@angular/material/dialog/testing';

export class ApiPortalSubscriptionEditHarness extends ComponentHarness {
  static hostSelector = '#subscription-edit';

  protected getSubscriptionDetail = (attr: string) => this.locatorFor(`#subscription-${attr}`)();
  protected getBackButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Go back to your subscriptions"]' }));
  protected getFooter = this.locatorFor('.subscription__footer');
  protected getBtnByText = (text: string) => this.locatorFor(MatButtonHarness.with({ text }))();
  protected getDialog = (selector: string) => this.locatorFor(MatDialogHarness.with({ selector }))();

  public async goBackToSubscriptionsList(): Promise<void> {
    return this.getBackButton().then((btn) => btn.click());
  }

  /**
   * DETAILS
   */
  public async getId(): Promise<string> {
    return this.getSubscriptionDetailText('id');
  }

  public async getPlan(): Promise<string> {
    return this.getSubscriptionDetailText('plan');
  }

  public async getStatus(): Promise<string> {
    return this.getSubscriptionDetailText('status');
  }

  public async getSubscribedBy(): Promise<string> {
    return this.getSubscriptionDetailText('subscribed-by');
  }

  public async getApplication(): Promise<string> {
    return this.getSubscriptionDetailText('application');
  }

  public async getSubscriberMessage(): Promise<string> {
    return this.getSubscriptionDetailText('subscriber-message');
  }

  public async getPublisherMessage(): Promise<string> {
    return this.getSubscriptionDetailText('publisher-message');
  }

  public async getCreatedAt(): Promise<string> {
    return this.getSubscriptionDetailText('created-at');
  }

  public async getProcessedAt(): Promise<string> {
    return this.getSubscriptionDetailText('processed-at');
  }

  public async getClosedAt(): Promise<string> {
    return this.getSubscriptionDetailText('closed-at');
  }

  public async getPausedAt(): Promise<string> {
    return this.getSubscriptionDetailText('paused-at');
  }

  public async getStartingAt(): Promise<string> {
    return this.getSubscriptionDetailText('starting-at');
  }

  public async getEndingAt(): Promise<string> {
    return this.getSubscriptionDetailText('ending-at');
  }

  public async getDomain(): Promise<string> {
    return this.getSubscriptionDetailText('domain');
  }

  /**
   * FOOTER
   */
  public async footerIsVisible(): Promise<boolean> {
    return this.isVisible(this.getFooter());
  }

  public async transferBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Transfer');
  }

  public async openTransferDialog(): Promise<void> {
    return this.getBtnByText('Transfer').then((btn) => btn.click());
  }

  public async getTransferDialog(): Promise<MatDialogHarness> {
    return this.getDialog('api-portal-subscription-transfer');
  }

  public async pauseBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Pause');
  }

  public async changeEndDateBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Change end date');
  }

  public async closeBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Close');
  }

  public async validateBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Validate subscription');
  }

  public async rejectBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Reject subscription');
  }

  private async btnIsVisible(text: string): Promise<boolean> {
    return this.isVisible(this.getBtnByText(text));
  }

  private async isVisible(element: Promise<unknown>): Promise<boolean> {
    return element.then((_) => true).catch((_) => false);
  }

  private async getSubscriptionDetailText(attr: string): Promise<string> {
    return this.getSubscriptionDetail(attr).then((res) => res.text());
  }
}
