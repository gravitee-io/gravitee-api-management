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
import { Api, ApiDefinition } from '@model/apis';
import { Plan, PlanSecurityType, PlanStatus, PlanValidation } from '@model/plan';

import { ApiImportFakers } from '@fakers/api-imports';
import { PolicyFakers } from '@fakers/policies';
import { ResourceFakers } from '@fakers/resources';
import { Step } from '@model/plan';
import { ApiImport } from '@model/api-imports';
import { faker } from '@faker-js/faker';

export class ApiFakers {
  static version() {
    const major = faker.number.int({ min: 1, max: 5 });
    const minor = faker.number.int({ min: 1, max: 10 });
    const patch = faker.number.int({ min: 1, max: 30 });
    return `${major}.${minor}.${patch}`;
  }

  static apiRating(): number {
    return faker.number.int({ min: 1, max: 5 });
  }

  static api(attributes?: Partial<Api>): Api {
    const name = faker.commerce.productName();
    return <Api>{
      ...attributes,
      contextPath: `/${faker.lorem.word()}-${faker.string.uuid()}-${Math.floor(Date.now() / 1000)}`,
      name,
      description: faker.lorem.words(10),
      version: ApiFakers.version(),
      endpoint: 'https://api.gravitee.io/echo',
    };
  }

  static apiDefinition(attributes?: Partial<ApiDefinition>): ApiDefinition {
    const name = faker.commerce.productName();
    return <ApiDefinition>{
      ...attributes,
      proxy: {
        context_path: `/${faker.lorem.word()}-${faker.string.uuid()}-${Math.floor(Date.now() / 1000)}`,
        endpoints: [
          {
            name: 'default',
            target: 'http://api.gravitee.io/echo',
            inherit: true,
          },
        ],
      },
      name,
      description: faker.lorem.words(10),
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
      description: faker.lorem.words(10),
    };
  }

  static oauth2Api(am_domainHrid: string, clientId: string, clientSecret: string, oauthConfig?: any): ApiImport {
    const fakeOauth2Resource = ResourceFakers.oauth2AmResource(am_domainHrid, clientId, clientSecret);
    let fakeOauth2Policy: Step;
    if (oauthConfig) {
      fakeOauth2Policy = PolicyFakers.oauth2Policy(fakeOauth2Resource.name, { configuration: oauthConfig });
    } else {
      fakeOauth2Policy = PolicyFakers.oauth2Policy(fakeOauth2Resource.name);
    }
    const fakeOauth2Flow = ApiImportFakers.flow({ pre: [fakeOauth2Policy] });
    const fakePlan = ApiImportFakers.plan({ security: PlanSecurityType.KEY_LESS, flows: [fakeOauth2Flow] });
    return ApiImportFakers.api({ plans: [fakePlan], resources: [fakeOauth2Resource] });
  }

  static jwtApi(jwtConfig?: any): ApiImport {
    const fakeJwtPolicy = PolicyFakers.jwtPolicy({ configuration: jwtConfig });
    const fakeJwtFlow = ApiImportFakers.flow({ pre: [fakeJwtPolicy] });
    const fakePlan = ApiImportFakers.plan({ security: PlanSecurityType.KEY_LESS, flows: [fakeJwtFlow] });
    return ApiImportFakers.api({ plans: [fakePlan] });
  }
}
