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
import { MatTableHarness } from '@angular/material/table/testing';

export class ApiSubscriptionEditHarness extends ComponentHarness {
  static hostSelector = '#subscription-edit';

  protected getSubscriptionDetail = (attr: string) => this.locatorFor(`#subscription-${attr}`)();
  protected getBackButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Go back to your subscriptions"]' }));
  protected getFooter = this.locatorFor('.subscription__footer');
  protected getBtnByText = (text: string) => this.locatorFor(MatButtonHarness.with({ text }))();
  protected getTable = this.locatorFor(MatTableHarness);
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

  public async getConsumerStatus(): Promise<string> {
    return this.getSubscriptionDetailText('consumer-status');
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

  public async pauseBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Pause');
  }

  public async openPauseDialog(): Promise<void> {
    return this.getBtnByText('Pause').then((btn) => btn.click());
  }

  public async resumeBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Resume');
  }

  public async openResumeDialog(): Promise<void> {
    return this.getBtnByText('Resume').then((btn) => btn.click());
  }

  public async changeEndDateBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Change end date');
  }

  public async openChangeEndDateDialog(): Promise<void> {
    return this.getBtnByText('Change end date').then((btn) => btn.click());
  }

  public async closeBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Close');
  }

  public async openCloseDialog(): Promise<void> {
    return this.getBtnByText('Close').then((btn) => btn.click());
  }

  public async validateBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Validate subscription');
  }

  public async openValidateDialog(): Promise<void> {
    return this.getBtnByText('Validate subscription').then((btn) => btn.click());
  }

  public async rejectBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Reject subscription');
  }

  public async openRejectDialog(): Promise<void> {
    return this.getBtnByText('Reject subscription').then((btn) => btn.click());
  }

  /**
   * API KEYS
   */

  public async renewApiKeyBtnIsVisible(): Promise<boolean> {
    return this.btnIsVisible('Renew');
  }

  public async openRenewApiKeyDialog(): Promise<void> {
    return this.getBtnByText('Renew').then((btn) => btn.click());
  }

  public async getApiKeyByRowIndex(index: number): Promise<string> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index].getCellTextByIndex({ columnName: 'key' }))
      .then((txt) => txt[0]);
  }

  public async getApiKeyEndDateByRowIndex(index: number): Promise<string> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index].getCellTextByIndex({ columnName: 'endDate' }))
      .then((txt) => txt[0]);
  }

  public async getRevokeApiKeyBtn(index: number): Promise<MatButtonHarness> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index].getCells({ columnName: 'actions' }))
      .then((cells) => cells[0].getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to revoke an API Key"]' })));
  }

  public async getExpireApiKeyBtn(index: number): Promise<MatButtonHarness> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index].getCells({ columnName: 'actions' }))
      .then((cells) => cells[0].getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to expire an API Key"]' })));
  }

  public async getReactivateApiKeyBtn(index: number): Promise<MatButtonHarness> {
    return this.getTable()
      .then((table) => table.getRows())
      .then((rows) => rows[index].getCells({ columnName: 'actions' }))
      .then((cells) => cells[0].getHarness(MatButtonHarness.with({ selector: '[aria-label="Button to reactivate an API Key"]' })));
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
