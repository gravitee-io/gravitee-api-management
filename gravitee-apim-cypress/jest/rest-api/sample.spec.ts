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

import { describe, expect, test } from '@jest/globals';
import { APIsApi } from '../../projects/management-webclient-sdk/src/lib/apis/APIsApi';
import { buildConfiguration } from '../../projects/configuration';

describe('Sample test with jest & sdk', () => {
  const apisResource = new APIsApi(buildConfiguration());

  test('should get all apis', async () => {
    const response = await apisResource.getApisRaw({ envId: 'DEFAULT', orgId: 'DEFAULT' });
    expect(response).not.toBeNull();
    expect(response.raw.status).toEqual(200);
  });
  
});
