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
import { afterAll, describe, expect, test } from '@jest/globals';
import { APIsApi, ApiV4 } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementV2AsApiUser } from '@gravitee/utils/configuration';
import { created, noContent, succeed } from '@lib/jest-utils';
import { MAPIV2ApisFaker } from '@gravitee/fixtures/management/MAPIV2ApisFaker';
import { ImagesUtils } from '@gravitee/utils/images';

const envId = 'DEFAULT';

const v2ApisResourceAsApiPublisher = new APIsApi(forManagementV2AsApiUser());

describe('API - V4 - Import - Gravitee Definition - With pictures', () => {
  describe('Create v4 API from import with pictures', () => {
    let importedApi: ApiV4;

    test('should import v4 API with pictures', async () => {
      importedApi = await created(
        v2ApisResourceAsApiPublisher.createApiWithImportDefinitionRaw({
          envId,
          exportApiV4: MAPIV2ApisFaker.apiImportV4({
            apiPicture: `data:image/png;base64,${ImagesUtils.fakeImage15x15}`,
            apiBackground: `data:image/png;base64,${ImagesUtils.fakeImage150x35}`,
          }),
        }),
      );
      expect(importedApi).toBeTruthy();
      expect(importedApi._links.pictureUrl).toBeTruthy();
      expect(importedApi._links.backgroundUrl).toBeTruthy();
    });

    test('should get API picture', async () => {
      const apiPictureResult = await succeed(
        v2ApisResourceAsApiPublisher.getApiPictureRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(apiPictureResult).toBeTruthy();
      expect(apiPictureResult.type).toStrictEqual('image/png');
      const base64Image = await ImagesUtils.blobToBase64(apiPictureResult);
      expect(base64Image).toStrictEqual(ImagesUtils.fakeImage15x15);
    });

    test('should get API background', async () => {
      const apiBackgroundResult = await succeed(
        v2ApisResourceAsApiPublisher.getApiBackgroundRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
      expect(apiBackgroundResult).toBeTruthy();
      expect(apiBackgroundResult.type).toStrictEqual('image/png');
      const base64Image = await ImagesUtils.blobToBase64(apiBackgroundResult);
      expect(base64Image).toStrictEqual(ImagesUtils.fakeImage150x35);
    });

    afterAll(async () => {
      await noContent(
        v2ApisResourceAsApiPublisher.deleteApiRaw({
          envId,
          apiId: importedApi.id,
        }),
      );
    });
  });
});
