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
import { MatCardHarness } from '@angular/material/card/testing';

export class MessageLogDetailDialogHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-message-log-detail-dialog';

  protected locateEntrypointCard = this.locatorFor(MatCardHarness.with({ text: /Entrypoint:.*/ }));
  protected locateEndpointCard = this.locatorFor(MatCardHarness.with({ text: /Endpoint:.*/ }));

  async isEntrypointCardShown(): Promise<boolean> {
    return this.locateEntrypointCard()
      .then(_ => true)
      .catch(_ => false);
  }

  async getEntrypointCard(): Promise<MatCardHarness> {
    return this.locateEntrypointCard();
  }

  async isEndpointCardShown(): Promise<boolean> {
    return this.locateEndpointCard()
      .then(_ => true)
      .catch(_ => false);
  }

  async getEndpointCard(): Promise<MatCardHarness> {
    return this.locateEndpointCard();
  }
}
