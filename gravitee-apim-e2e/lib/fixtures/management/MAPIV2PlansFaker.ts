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
import faker from '@faker-js/faker';
import { PlanValidation, PlanSecurityType, CreatePlanV4, PlanMode, CreatePlan } from '../../management-v2-webclient-sdk/src/lib';

export class MAPIV2PlansFaker {
  static newPlanV4(attributes?: Partial<CreatePlan>): CreatePlan {
    const name = faker.commerce.productName();
    const description = faker.lorem.words(10);

    return {
      name,
      description,
      definitionVersion: 'V4',
      validation: PlanValidation.AUTO,
      security: { type: PlanSecurityType.KEY_LESS },
      mode: PlanMode.STANDARD,
      order: 1,
      characteristics: [],
      flows: [],
      ...attributes,
    };
  }
}
