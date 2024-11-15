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

import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class GioFormListenersKafkaHostPortHarness extends ComponentHarness {
  public static hostSelector = 'gio-form-listeners-kafka-host-port';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `GioFormListenersContextPathHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  public static with(options: BaseHarnessFilters = {}): HarnessPredicate<GioFormListenersKafkaHostPortHarness> {
    return new HarnessPredicate(GioFormListenersKafkaHostPortHarness, options);
  }

  protected hostInput = this.locatorFor(MatInputHarness.with({ ancestor: '.kafka-configuration__host' }));
  protected portInput = this.locatorFor(MatInputHarness.with({ ancestor: '.kafka-configuration__port' }));

  public getHostInput(): Promise<MatInputHarness> {
    return this.hostInput();
  }

  public getPortInput(): Promise<MatInputHarness> {
    return this.portInput();
  }
}
