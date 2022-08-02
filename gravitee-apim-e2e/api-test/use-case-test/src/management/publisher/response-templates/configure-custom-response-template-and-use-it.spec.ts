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
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsApiUser } from '@client-conf/*';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanStatus } from '@management-models/PlanStatus';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { teardownApisAndApplications } from '@lib/management';
import { fetchGatewayBadRequest, fetchGatewaySuccess } from '@lib/gateway';
import { PathOperatorOperatorEnum } from '@management-models/PathOperator';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());

describe('Configure custom response templates and use it', () => {
  describe('Set up & use multiple template definitions in single response template', () => {
    let createdApi: ApiEntity;

    beforeAll(async () => {
      createdApi = await apisResource.importApiDefinition({
        envId,
        orgId,
        body: ApisFaker.apiImport({
          plans: [PlansFaker.plan({ security: PlanSecurityType.KEYLESS, status: PlanStatus.PUBLISHED })],
          flows: [
            {
              name: '',
              path_operator: {
                path: '/',
                operator: PathOperatorOperatorEnum.STARTSWITH,
              },
              condition: '',
              consumers: [],
              methods: [],
              pre: [
                {
                  name: 'Rate limit',
                  description: '',
                  enabled: true,
                  policy: 'rate-limit',
                  configuration: {
                    async: false,
                    addHeaders: false,
                    rate: {
                      periodTime: 20,
                      limit: 1,
                      periodTimeUnit: 'SECONDS',
                      key: '',
                    },
                  },
                },
              ],
              post: [],
              enabled: true,
            },
          ],
          response_templates: {
            RATE_LIMIT_TOO_MANY_REQUESTS: {
              '*/*': {
                status: 400,
                headers: { 'test-header': 'custom header general' },
                body: '{"message": "error template for general content"}',
              },
              'application/json': {
                status: 400,
                headers: { 'test-header': 'custom header json' },
                body: '{"message": "error template for json content"}',
              },
            },
          },
        }),
      });

      // start API
      await apisResource.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.START,
      });

      // make sure it's working and exhaust rate-limit (= one request)
      await fetchGatewaySuccess({
        contextPath: createdApi.context_path,
        headers: { Accept: 'application/json' },
      });
    });

    test('should trigger custom response template for json content-type', async () => {
      const response = await fetchGatewayBadRequest({
        // status (400)
        contextPath: createdApi.context_path,
        headers: { Accept: 'application/json' },
      }).then((res) => {
        expect(res.headers.get('test-header')).toBe('custom header json'); // header
        return res.json();
      });
      expect(response.message).toBe('error template for json content'); // body
    });

    test('should trigger custom response template for general content-type (*/*)', async () => {
      const response = await fetchGatewayBadRequest({
        // status (400)
        contextPath: createdApi.context_path,
        headers: { Accept: 'text/xml' },
      }).then((res) => {
        expect(res.headers.get('test-header')).toBe('custom header general'); // header
        return res.text();
      });
      expect(response).toContain('error template for general content'); // body
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi.id]);
    });
  });

  describe('Set up & use Default response template', () => {
    let createdApi: ApiEntity;

    beforeAll(async () => {
      createdApi = await apisResource.importApiDefinition({
        envId,
        orgId,
        body: ApisFaker.apiImport({
          plans: [PlansFaker.plan({ security: PlanSecurityType.KEYLESS, status: PlanStatus.PUBLISHED })],
          flows: [
            {
              name: '',
              path_operator: {
                path: '/',
                operator: PathOperatorOperatorEnum.STARTSWITH,
              },
              condition: '',
              consumers: [],
              methods: [],
              pre: [
                {
                  name: 'Rate limit',
                  description: '',
                  enabled: true,
                  policy: 'rate-limit',
                  configuration: {
                    async: false,
                    addHeaders: false,
                    rate: {
                      periodTime: 20,
                      limit: 1,
                      periodTimeUnit: 'SECONDS',
                      key: '',
                    },
                  },
                },
              ],
              post: [],
              enabled: true,
            },
          ],
          response_templates: {
            DEFAULT: {
              '*/*': {
                status: 400,
                headers: { 'test-header': 'custom header default' },
                body: '{"message": "default error template"}',
              },
            },
          },
        }),
      });

      // start API
      await apisResource.doApiLifecycleAction({
        envId,
        orgId,
        api: createdApi.id,
        action: LifecycleAction.START,
      });

      // make sure it's working and exhaust rate-limit (= one request)
      await fetchGatewaySuccess({ contextPath: createdApi.context_path });
    });

    test('should trigger Default response template', async () => {
      const response = await fetchGatewayBadRequest({
        // status (400)
        contextPath: createdApi.context_path,
      }).then((res) => {
        expect(res.headers.get('test-header')).toBe('custom header default'); // header
        return res.json();
      });
      expect(response.message).toBe('default error template'); // body
    });

    afterAll(async () => {
      await teardownApisAndApplications(orgId, envId, [createdApi.id]);
    });
  });
});
