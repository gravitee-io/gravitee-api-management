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
import { MatDatepickerInputHarness } from '@angular/material/datepicker/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class MoreFiltersDialogHarness extends ComponentHarness {
  public static hostSelector = 'app-more-filters-dialog';

  protected locateDatePickers = this.locatorForAll(MatDatepickerInputHarness);
  protected locateApplyButton = this.locatorFor(MatButtonHarness.with({ text: 'Apply' }));
  protected locateCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  protected locateRequestId = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Filter by Request ID"]' }));
  protected locateTransactionId = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Filter by Transaction ID"]' }));
  protected locateHttpStatus = this.locatorFor(MatSelectHarness.with({ selector: '[aria-label="Filter by HTTP Status"]' }));
  protected locateMessageText = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Filter by Message Text"]' }));

  async getStartDatePicker(): Promise<MatDatepickerInputHarness> {
    return await this.locateStartDatePicker();
  }

  async getEndDatePicker(): Promise<MatDatepickerInputHarness> {
    return await this.locateEndDatePicker();
  }

  async getRequestIdInput(): Promise<MatInputHarness> {
    return await this.locateRequestId();
  }

  async getTransactionIdInput(): Promise<MatInputHarness> {
    return await this.locateTransactionId();
  }

  async getHttpStatusSelection(): Promise<MatSelectHarness> {
    return await this.locateHttpStatus();
  }

  async getMessageTextInput(): Promise<MatInputHarness> {
    return await this.locateMessageText();
  }

  async applyFilters(): Promise<void> {
    return await this.locateApplyButton().then(btn => btn.click());
  }

  async cancel(): Promise<void> {
    return await this.locateCancelButton().then(btn => btn.click());
  }

  protected locateStartDatePicker = async () => await this.locateDatePickers().then(pickers => pickers[0]);
  protected locateEndDatePicker = async () => await this.locateDatePickers().then(pickers => pickers[1]);
}
