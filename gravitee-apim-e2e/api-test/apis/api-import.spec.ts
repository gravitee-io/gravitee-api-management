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
import { describe, expect, test, it, afterEach, beforeAll, afterAll } from '@jest/globals';

import * as swaggerv2 from '../resources/petstore_swaggerv2.json';
import * as openapiv3 from '../resources/petstore_openapiv3.json';
import { src as wsdlapi } from '../resources/wsdl.xml';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { ImportSwaggerDescriptorEntity } from '@management-models/ImportSwaggerDescriptorEntity';
import { Format } from '@management-models/Format';
import { Type } from '@management-models/Type';
import { APIPagesApi } from '@management-apis/APIPagesApi';
import { fail, succeed } from '../../lib/jest-utils';
import { ApiEntityStateEnum } from '@management-models/ApiEntity';
import { Visibility } from '@management-models/Visibility';

const apisResourceAdmin = new APIsApi(forManagementAsAdminUser());
const apisResourceApiUser = new APIsApi(forManagementAsApiUser());
const pagesResourcesApiUser = new APIPagesApi(forManagementAsApiUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const definitionVersion = '2.0.0';

describe('Api import', () => {
  const apiImportArray = [
    { name: 'Swagger v2 file', payload: JSON.stringify(swaggerv2), type: Type.INLINE, format: Format.API, expectedFlowsLength: 20 },
    { name: 'OpenAPI v3 file', payload: JSON.stringify(openapiv3), type: Type.INLINE, format: Format.API, expectedFlowsLength: 19 },
    { name: 'WSDL file', payload: wsdlapi, type: Type.INLINE, format: Format.WSDL, expectedFlowsLength: 42 },
    // Use only on local env, expose to flaky and webservices.oorsprong.org doesn't belong to gravitee !
    // { name:'OpenAPI url', payload: 'https://docs.gravitee.io/apim/3.x/management-api/3.17/swagger.json', type: Type.URL, format: Format.API },
    // { name:'WSDL url', payload: 'http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL', type: Type.URL, format: Format.WSDL },
  ];

  describe.each(apiImportArray)('API import via $name', ({ payload, type, format, expectedFlowsLength }) => {
    let apiId: string;
    const importSwaggerDescriptorEntity: ImportSwaggerDescriptorEntity = {
      with_policies: [],
      format,
      type,
      payload,
    };

    afterEach(async () => await apisResourceAdmin.deleteApi({ orgId, envId, api: apiId }));

    test('should import API without creating a documentation', async () => {
      const api = await succeed(
        apisResourceApiUser.importSwaggerApiRaw({
          orgId,
          envId,
          definitionVersion,
          importSwaggerDescriptorEntity,
        }),
        201,
      );

      apiId = api.id;
      expect(api.id).not.toEqual('');
      expect(api.visibility).toEqual(Visibility.PRIVATE);
      expect(api.state).toEqual(ApiEntityStateEnum.STOPPED);

      const pages = await succeed(pagesResourcesApiUser.getApiPagesRaw({ orgId, envId, api: api.id }));
      expect(pages).toHaveLength(1);
    });

    test('should import API and create a swagger documentation', async () => {
      const api = await succeed(
        apisResourceApiUser.importSwaggerApiRaw({
          orgId,
          envId,
          definitionVersion,
          importSwaggerDescriptorEntity: { ...importSwaggerDescriptorEntity, with_documentation: true },
        }),
        201,
      );

      apiId = api.id;
      expect(api.id).not.toEqual('');
      expect(api.visibility).toEqual(Visibility.PRIVATE);
      expect(api.state).toEqual(ApiEntityStateEnum.STOPPED);

      const pages = await succeed(pagesResourcesApiUser.getApiPagesRaw({ orgId, envId, api: api.id }));

      expect(pages).toHaveLength(2);
      expect(pages[1].id).not.toEqual('');
      expect(pages[1].type).toEqual('SWAGGER');
      expect(pages[1].content).not.toEqual('');
    });

    test('should fail to import the same Swagger API again', async () => {
      const api = await succeed(
        apisResourceApiUser.importSwaggerApiRaw({
          orgId,
          envId,
          definitionVersion,
          importSwaggerDescriptorEntity,
        }),
        201,
      );

      apiId = api.id;

      await fail(
        apisResourceApiUser.importSwaggerApiRaw({
          orgId,
          envId,
          definitionVersion,
          importSwaggerDescriptorEntity,
        }),
        400,
        `The path [${api.context_path}/] is already covered by an other API.`,
      );
    });

    test('should import API and create a path (to add policies) for every declared Swagger path', async () => {
      const api = await succeed(
        apisResourceApiUser.importSwaggerApiRaw({
          orgId,
          envId,
          definitionVersion,
          importSwaggerDescriptorEntity: {
            ...importSwaggerDescriptorEntity,
            with_policy_paths: true,
          },
        }),
        201,
      );

      apiId = api.id;
      expect(api.flows).toHaveLength(expectedFlowsLength);
    });
  });

  test('should fail when trying to import an empty file/URL', async () => {
    await fail(
      apisResourceApiUser.importSwaggerApi({
        orgId,
        envId,
        definitionVersion,
        importSwaggerDescriptorEntity: { payload: '' },
      }),
      500,
    );
  });
});
