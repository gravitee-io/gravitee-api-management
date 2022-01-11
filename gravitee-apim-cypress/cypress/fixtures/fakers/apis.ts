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
import * as faker from 'faker';
import { Api, ApiDefinition } from '@model/apis';
import { Plan, PlanSecurityType, PlanStatus, PlanValidation } from '@model/plan';

export class ApiFakers {
  static version() {
    const major = faker.datatype.number({ min: 1, max: 5 });
    const minor = faker.datatype.number({ min: 1, max: 10 });
    const patch = faker.datatype.number({ min: 1, max: 30 });
    return `${major}.${minor}.${patch}`;
  }

  static apiRating(): number {
    return faker.datatype.number({ min: 1, max: 5 });
  }

  static api(attributes?: Partial<Api>): Api {
    const name = faker.commerce.productName();
    return <Api>{
      ...attributes,
      contextPath: `/${faker.random.word()}-${faker.datatype.uuid()}-${Math.floor(Date.now() / 1000)}`,
      name,
      description: faker.commerce.productDescription(),
      version: ApiFakers.version(),
      endpoint: 'https://api.gravitee.io/echo',
    };
  }

  static apiDefinition(attributes?: Partial<ApiDefinition>): ApiDefinition {
    const name = faker.commerce.productName();
    return <ApiDefinition>{
      ...attributes,
      proxy: {
        context_path: `/${faker.random.word()}-${faker.datatype.uuid()}-${Math.floor(Date.now() / 1000)}`,
        endpoints: [
          {
            name: 'default',
            target: 'http://api.gravitee.io/echo',
            inherit: true,
          },
        ],
      },
      name,
      description: faker.commerce.productDescription(),
      version: ApiFakers.version(),
    };
  }

  static plan(attributes?: Partial<Plan>): Plan {
    return <Plan>{
      validation: PlanValidation.AUTO,
      securityDefinition: PlanSecurityType.API_KEY,
      status: PlanStatus.PUBLISHED,
      ...attributes,
      name: faker.commerce.productName(),
      description: faker.commerce.productDescription(),
    };
  }
}
