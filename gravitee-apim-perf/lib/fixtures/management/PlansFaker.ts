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
import { PlanEntity } from '@management-models/PlanEntity';
import { PlanValidationType } from '@management-models/PlanValidationType';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanType } from '@management-models/PlanType';
import { PlanStatus } from '@management-models/PlanStatus';
import { NewPlanEntity } from '@management-models/NewPlanEntity';
import { Plan } from '@management-models/Plan';

export class PlansFaker {
  static plan(attributes?: Partial<PlanEntity>): PlanEntity {
    const name = faker.commerce.productName();
    const description = faker.commerce.productDescription();

    return {
      name,
      description,
      validation: PlanValidationType.AUTO,
      security: PlanSecurityType.KEYLESS,
      type: PlanType.API,
      status: PlanStatus.STAGING,
      order: 1,
      characteristics: [],
      paths: {},
      flows: [],
      comment_required: false,
      ...attributes,
    };
  }

  static newPlan(attributes?: Partial<NewPlanEntity>): NewPlanEntity {
    const name = faker.commerce.productName();
    const description = faker.commerce.productDescription();

    return {
      name,
      description,
      validation: PlanValidationType.AUTO,
      security: PlanSecurityType.KEYLESS,
      type: PlanType.API,
      status: PlanStatus.STAGING,
      order: 1,
      characteristics: [],
      paths: {},
      flows: [],
      comment_required: false,
      ...attributes,
    };
  }

  static aPlan(attributes?: Partial<PlanEntity>): Plan {
    const name = faker.commerce.productName();
    const description = faker.commerce.productDescription();

    return {
      name,
      description,
      validation: PlanValidationType.AUTO,
      security: PlanSecurityType.KEYLESS,
      type: PlanType.API,
      status: PlanStatus.STAGING,
      order: 1,
      characteristics: [],
      paths: {},
      flows: [],
      comment_required: false,
      ...attributes,
    };
  }
}
