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

import { ApiImportFakers } from "@fakers/api-imports";
import { PolicyFakers } from "@fakers/policies";
import { ResourceFakers } from "@fakers/resources";
import { API_PUBLISHER_USER } from "@fakers/users/users";
import { Step, PlanSecurityType } from "@model/plan";
import { importCreateApi } from "./management/api-management-commands";

export function createOauth2Api(am_domainHrid: string, clientId: string, clientSecret: string, oauthConfig?: any) {
    const fakeOauth2Resource = ResourceFakers.oauth2AmResource(am_domainHrid, clientId, clientSecret);
    let fakeOauth2Policy: Step;
    if (oauthConfig) {
      fakeOauth2Policy = PolicyFakers.oauth2Policy(fakeOauth2Resource.name, { configuration: oauthConfig });
    } else {
      fakeOauth2Policy = PolicyFakers.oauth2Policy(fakeOauth2Resource.name);
    }
    const fakeOauth2Flow = ApiImportFakers.flow({ pre: [fakeOauth2Policy] });
    const fakePlan = ApiImportFakers.plan({ security: PlanSecurityType.KEY_LESS, flows: [fakeOauth2Flow] });
    const fakeApi = ApiImportFakers.api({ plans: [fakePlan], resources: [fakeOauth2Resource] });
    return importCreateApi(API_PUBLISHER_USER, fakeApi);
  }
  