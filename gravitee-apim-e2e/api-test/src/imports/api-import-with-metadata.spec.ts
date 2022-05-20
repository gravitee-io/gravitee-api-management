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
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser } from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { succeed } from '@lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsAdminUser = new APIsApi(forManagementAsAdminUser());

describe('API - Imports with metadata', () => {
  // API with metadata having key already that already exists on an other API
  const firstApiId = 'b68e1a9c-a344-460d-b0ac-1d86d61b70cf';
  const firstExpectedApiId = 'd63a3aa0-46ba-3a93-9104-00ae4240645e';
  const firstApiMetadata = {
    key: 'team',
    name: 'team',
    format: 'STRING',
    value: 'API Management',
  };
  const firstApi = ApisFaker.apiImport({
    id: firstApiId,
    metadata: [firstApiMetadata],
  });

  const secondApiId = '5668f9f0-12af-4541-b834-c374faedfb57';
  const secondExpectedApiId = 'b639cf39-7d66-3b36-af37-656233c4794b';
  const secondApiMetadata = {
    key: 'team',
    name: 'team',
    format: 'STRING',
    value: 'Access Management',
  };
  const secondApi = ApisFaker.apiImport({
    id: secondApiId,
    metadata: [secondApiMetadata],
  });

  // API with metadata having key that does not yet exist
  const thirdApiId = 'bc1287cb-b732-4ba1-b609-1e34d375b585';
  const thirdExpectedApiId = 'c954073c-0812-3544-b313-ad4f0001ffac';
  const thirdExpectedMetadata = {
    key: 'team',
    name: 'team',
    format: 'STRING',
    value: 'API Management',
  };

  const thirdApi = ApisFaker.apiImport({
    id: thirdApiId,
    metadata: [thirdExpectedMetadata],
  });

  // Api with metadata having an undefined key
  const fourthApiId = '4d73b285-5b87-4186-928e-f6f6240708f3';
  const fourthExpectedApiId = '08ee5d81-a6b8-3562-aaf6-b2c1313398cd';
  const fourthExpectedMetadata = {
    name: 'team',
    format: 'STRING',
    value: 'QA',
  };
  const fourthApi = ApisFaker.apiImport({
    id: fourthApiId,
    metadata: [fourthExpectedMetadata],
  });

  beforeAll(async () => {
    const createdApis = [firstApi, secondApi, thirdApi, fourthApi].map(
      async (apiId) =>
        await apisResourceAsAdminUser.importApiDefinition({
          envId,
          orgId,
          body: apiId,
        }),
    );
    await Promise.all(createdApis);
  });

  test.each`
    expectedApiId          | expectedMetadata
    ${firstExpectedApiId}  | ${firstApiMetadata}
    ${secondExpectedApiId} | ${secondApiMetadata}
    ${thirdExpectedApiId}  | ${thirdExpectedMetadata}
    ${fourthExpectedApiId} | ${fourthExpectedMetadata}
  `('should get API metadata for the API $expectedApiId', async ({ expectedApiId, expectedMetadata }) => {
    const metadata = await succeed(apisResourceAsAdminUser.getApiMetadatasRaw({ orgId, envId, api: expectedApiId }));
    expect(metadata).toBeTruthy();
    expect(metadata).toContainEqual({ apiId: expectedApiId, ...expectedMetadata });
  });

  afterAll(async () => {
    const deletedApis = [firstExpectedApiId, secondExpectedApiId, thirdExpectedApiId].map(
      async (apiId) =>
        await apisResourceAsAdminUser.deleteApi({
          envId,
          orgId,
          api: apiId,
        }),
    );
    await Promise.all(deletedApis);
  });
});
