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

import { beforeAll, describe, expect, test } from '@jest/globals';
import { GatewayApi } from '@management-apis/GatewayApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { fail } from '../../lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';
const gateway_admin = new GatewayApi(forManagementAsAdminUser());
const gateway_user = new GatewayApi(forManagementAsApiUser());

describe('Gateway tests', () => {
  describe('Get all gateway instances', () => {
    test('should get a list of gateways as response that complies with response schema', async () => {
      const response = await gateway_admin.getInstancesRaw({ envId, orgId });
      expect(response.raw.status).toBe(200);

      const gatewayInstances = await response.value();
      expect(gatewayInstances).toHaveProperty('content');
      expect(gatewayInstances).toHaveProperty('pageElements');
      expect(gatewayInstances).toHaveProperty('pageNumber');
      expect(gatewayInstances).toHaveProperty('totalElements');

      const instanceListItem = {
        event: expect.any(String),
        id: expect.any(String),
        hostname: expect.any(String),
        ip: expect.any(String),
        port: expect.any(String),
        version: expect.any(String),
        state: expect.any(String),
        started_at: expect.any(Date),
        last_heartbeat_at: expect.any(Date),
        operating_system_name: expect.any(String),
      };
      expect(gatewayInstances.content[0]).toMatchObject(instanceListItem);
    });

    test('should not get a response when using non-admin permissions', async () => {
      await fail(gateway_user.getInstancesRaw({ envId, orgId }), 403, 'You do not have sufficient rights to access this resource');
    });
  });

  describe('Get infos for specific gateway instance', () => {
    let instanceId: string;

    beforeAll(async () => {
      const gatewayInstances = await gateway_admin.getInstances({ envId, orgId });
      instanceId = gatewayInstances.content[0].event;
    });

    test('should get a single instance as response that complies with response schema', async () => {
      const gatewayInstance = await gateway_admin.getInstance({ instance: instanceId, envId, orgId });
      const instanceListItem = {
        event: expect.any(String),
        id: expect.any(String),
        hostname: expect.any(String),
        ip: expect.any(String),
        port: expect.any(String),
        tenant: undefined,
        version: expect.any(String),
        environments: expect.any(Array),
        state: expect.any(String),
        systemProperties: expect.any(Object),
        plugins: expect.any(Array),
        started_at: expect.any(Date),
        last_heartbeat_at: expect.any(Date),
        environments_hrids: expect.any(Array),
        organizations_hrids: expect.any(Array),
      };
      expect(gatewayInstance).toMatchObject(instanceListItem);
    });

    test('should not get a response when using non-admin permissions', async () => {
      await fail(
        gateway_user.getInstanceRaw({ instance: instanceId, envId, orgId }),
        403,
        'You do not have sufficient rights to access this resource',
      );
    });
  });
});
