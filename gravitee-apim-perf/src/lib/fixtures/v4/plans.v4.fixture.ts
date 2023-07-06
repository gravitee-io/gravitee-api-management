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
import { randomString } from '@helpers/random.helper';
import { NewPlanEntityV4, NewPlanEntityV4StatusEnum, PlanSecurityTypeV4 } from '@models/v4/NewPlanEntityV4';
import { PlanEntityV4DefinitionVersionEnum, PlanModeV4, PlanTypeV4, PlanValidationTypeV4 } from '@models/v4/PlanEntityV4';

export class PlansV4Fixture {
  static newPlan(attributes?: Partial<NewPlanEntityV4>): NewPlanEntityV4 {
    const name = randomString();
    const description = randomString();
    const needSecurityPlan = !attributes || (attributes['mode'] && attributes['mode'] == 'STANDARD');
    return {
      name,
      description,
      definitionVersion: PlanEntityV4DefinitionVersionEnum._V4,
      validation: PlanValidationTypeV4.AUTO,
      security: needSecurityPlan ? { type: PlanSecurityTypeV4.KEY_LESS } : null,
      mode: PlanModeV4.STANDARD,
      type: PlanTypeV4.API,
      status: NewPlanEntityV4StatusEnum.STAGING,
      order: 1,
      characteristics: [],
      flows: [],
      ...attributes,
    };
  }
}
