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
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { ApiPlanFormHarness } from '../../../api/component/plan/api-plan-form.harness';

export class ApiProductPlanEditComponentHarness extends ComponentHarness {
  static hostSelector = 'api-product-plan-edit';

  async getSaveBar(): Promise<GioSaveBarHarness | null> {
    return this.locatorForOptional(GioSaveBarHarness)();
  }

  async isSaveBarVisible(): Promise<boolean> {
    const bar = await this.getSaveBar();
    return bar !== null;
  }

  async getPlanForm(): Promise<ApiPlanFormHarness> {
    return this.locatorFor(ApiPlanFormHarness)();
  }

  /** Clicks the Next step button in the save bar (create mode stepper). */
  async clickNextStep(): Promise<void> {
    const nextBtn = await this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="api_product_plans_nextstep"]' }))();
    return nextBtn.click();
  }

  /** Sets the plan name in the plan form (for achieving valid/invalid state via UI). */
  async setPlanName(name: string): Promise<void> {
    const planForm = await this.getPlanForm();
    const nameInput = await planForm.getNameInput();
    return nameInput.setValue(name);
  }
}
