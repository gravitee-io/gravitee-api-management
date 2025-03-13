/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { MatInputHarness } from '@angular/material/input/testing';

export class ConsumerConfigurationComponentHarness extends ComponentHarness {
  public static hostSelector = 'app-consumer-configuration';

  async getInputTextFromControlName(formControlName: string): Promise<string> {
    const input: MatInputHarness = await this.locatorFor(MatInputHarness.with({ selector: `[formcontrolname='${formControlName}']` }))();
    return input.getValue();
  }
}
