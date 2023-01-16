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
import { MatListOptionHarness } from '@angular/material/list/testing';

export class ApiCreationV4Step2Harness extends ComponentHarness {
  static hostSelector = 'api-creation-v4-step-2';

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

  protected getListOptionByValue = (id: string) => this.locatorFor(MatListOptionHarness.with({ selector: `[ng-reflect-value="${id}"]` }))();

  async clickPrevious(): Promise<void> {
    return this.getPreviousButton().then((elt) => elt.click());
  }

  async clickValidate() {
    return this.getValidateButton().then((elt) => elt.click());
  }

  async markEntrypointSelectedById(id: string): Promise<void> {
    const listOption = await this.getListOptionByValue(id);
    await listOption.select();
  }

  async fillStep(entrypointIds: string[]): Promise<void> {
    await Promise.all(entrypointIds.map((id) => this.markEntrypointSelectedById(id)));
  }
}
