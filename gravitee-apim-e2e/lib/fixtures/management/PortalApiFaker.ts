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
import { RatingInput } from '@portal-models/RatingInput';

export class PortalApiFaker {
  static newRatingInput(): RatingInput {
    return {
      title: faker.random.word(),
      comment: `${faker.commerce.productDescription()}`,
      value: faker.datatype.number({ min: 1, max: 5, precision: 1 }),
    };
  }
}
