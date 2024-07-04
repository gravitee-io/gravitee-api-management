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

import { PlanCardHarness } from '../../../../components/subscribe/plan-card/plan-card.harness';

export class SubscribeToApiChoosePlanHarness extends ComponentHarness {
  public static hostSelector = 'app-subscribe-to-api-choose-plan';

  public async selectPlanByPlanId(planId: string): Promise<void> {
    return await this.locatePlanCardByPlanId(planId).then(planCard => planCard.select());
  }

  public async isPlanSelected(planId: string): Promise<boolean> {
    return await this.locatePlanCardByPlanId(planId).then(planCard => planCard.isSelected());
  }

  public async isPlanDisabled(planId: string): Promise<boolean> {
    return await this.locatePlanCardByPlanId(planId).then(planCard => planCard.isDisabled());
  }

  protected locatePlanCardByPlanId = (planId: string) => this.locatorFor(PlanCardHarness.with({ selector: `#${planId}` }))();
}
