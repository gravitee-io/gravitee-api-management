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
import { PlanEntity } from '@gravitee/management-webclient-sdk/src/lib/models/PlanEntity';
import { PlanValidationType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanValidationType';
import { PlanSecurityType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanSecurityType';
import { PlanType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanType';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { NewPlanEntity } from '@gravitee/management-webclient-sdk/src/lib/models/NewPlanEntity';
import { Plan } from '@gravitee/management-webclient-sdk/src/lib/models/Plan';
import { MetadataFormat, NewMetadataEntity } from '../../management-webclient-sdk/src/lib/models';

export class MetadataFaker {
  static newMetadata(attributes?: Partial<NewMetadataEntity>): NewMetadataEntity {
    return {
      name: faker.lorem.words(1),
      format: MetadataFormat.STRING,
      value: faker.lorem.words(1),
      hidden: false,
      ...attributes,
    };
  }
}
