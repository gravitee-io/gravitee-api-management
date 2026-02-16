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
import { MatRadioGroupHarness } from '@angular/material/radio/testing';

import { GioLicenseBannerHarness } from '../../../../../shared/components/gio-license-banner/gio-license-banner.harness';

export class ApiEndpointGroupSelectionHarness extends ComponentHarness {
  static hostSelector = 'api-endpoints-group-selection';

  private getEndpointsRadioGroup = this.locatorFor(MatRadioGroupHarness);

  public getLicenseBanner = this.locatorForOptional(GioLicenseBannerHarness);

  async selectEndpoint(id: string) {
    const radioGroup = (await this.getEndpointsRadioGroup()) as MatRadioGroupHarness;
    return await radioGroup.checkRadioButton({
      selector: `[data-testid="${id}"]`,
    });
  }

  async getAllEndpointIds(): Promise<string[]> {
    const group = await this.getEndpointsRadioGroup();
    const buttons = await group.getRadioButtons();
    return Promise.all(buttons.map(b => b.getValue()));
  }
}
