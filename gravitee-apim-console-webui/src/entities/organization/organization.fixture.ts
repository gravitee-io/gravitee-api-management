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
import { Organization } from './organization';

import { fakeFlow } from '../flow/flow.fixture';

export function fakeOrganization(attributes?: Partial<Organization>): Organization {
  const base: Organization = {
    id: 'org#1',
    cockpitId: 'org#1',
    hrids: ['org_1'],
    name: 'Organization 1',
    description: 'Organization 1 - Description',
    domainRestrictions: [],
    flowMode: 'DEFAULT',
    flows: [fakeFlow()],
  };

  return {
    ...base,
    ...attributes,
  };
}
