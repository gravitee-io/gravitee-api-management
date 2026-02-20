/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { MatProgressSpinnerHarness } from '@angular/material/progress-spinner/testing';

import { BrokersHarness } from '../brokers/brokers.harness';

export class KafkaExplorerHarness extends ComponentHarness {
  static hostSelector = 'gke-kafka-explorer';

  private readonly getSpinner = this.locatorForOptional(MatProgressSpinnerHarness);
  private readonly getBrokers = this.locatorForOptional(BrokersHarness);
  private readonly getErrorBanner = this.locatorForOptional('.kafka-explorer__error');
  private readonly getTopbar = this.locatorForOptional('.kafka-explorer__topbar');

  async isLoading() {
    return (await this.getSpinner()) !== null;
  }

  async getErrorMessage() {
    const error = await this.getErrorBanner();
    return error ? error.text() : null;
  }

  async getTopbarText() {
    const topbar = await this.getTopbar();
    return topbar ? topbar.text() : null;
  }

  async getBrokersHarness() {
    return this.getBrokers();
  }
}
