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

export class GioChartPieHarness extends ComponentHarness {
  static hostSelector = '.gio-chart-pie';

  protected getPieChart = this.locatorForOptional('.gio-chart-pie__chart');
  protected getNoDataDisplayed = this.locatorForOptional('.gio-chart-pie__no-data');

  async hasNoData(): Promise<boolean> {
    return this.getNoDataDisplayed()
      .then(v => !!v)
      .catch(_ => false);
  }

  async displaysChart() {
    return this.getPieChart()
      .then(v => !!v)
      .catch(_ => false);
  }
}
