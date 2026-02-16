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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class ApiEndpointGroupGeneralHarness extends ComponentHarness {
  static hostSelector = 'api-endpoint-group-general';
  private getEndpointGroupNameInput = this.locatorFor(MatInputHarness.with({ selector: '[aria-label="Endpoints group name input"]' }));
  private getEndpointGroupLoadBalancerSelector = this.locatorForOptional(
    MatSelectHarness.with({ selector: '[aria-label="Load balancing algorithm"]' }),
  );
  public async getNameValue(): Promise<string> {
    return this.getEndpointGroupNameInput().then(input => input.getValue());
  }

  public setNameValue(inputValue: string): Promise<void> {
    return this.getEndpointGroupNameInput().then(input => input.setValue(inputValue));
  }

  public async getLoadBalancerValue(): Promise<string> {
    return this.getEndpointGroupLoadBalancerSelector().then(selector => selector.getValueText());
  }

  public async setLoadBalancerValue(selectorValue: string): Promise<void> {
    return this.getEndpointGroupLoadBalancerSelector().then(input => input.clickOptions({ text: selectorValue }));
  }
}
