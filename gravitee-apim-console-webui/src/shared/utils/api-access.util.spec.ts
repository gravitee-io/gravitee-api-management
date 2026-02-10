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

import { getApiAccess } from './api-access.util';

import { fakeApiV2, fakeApiV4, fakeNativeKafkaApiV4 } from '../../entities/management-api-v2';

describe('getApiAccess', () => {
  it('should return v2 virtualHosts when present', () => {
    const api = fakeApiV2({
      proxy: {
        ...fakeApiV2().proxy,
        virtualHosts: [
          { host: 'example.com', path: '/planets', overrideEntrypoint: true },
          { host: 'api.example.com', path: '/v2', overrideEntrypoint: true },
        ],
      },
    });

    expect(getApiAccess(api)).toEqual(['example.com/planets', 'api.example.com/v2']);
  });

  it('should return v2 contextPath when no virtualHosts', () => {
    const api = fakeApiV2({ proxy: { ...fakeApiV2().proxy, virtualHosts: [] }, contextPath: '/ctx' });

    expect(getApiAccess(api)).toEqual(['/ctx']);
  });

  it('should return v4 native kafka host list and include port when provided', () => {
    const api = fakeNativeKafkaApiV4({
      listeners: [
        {
          type: 'KAFKA',
          host: 'kafka.local',
          port: 9092,
        },
        {
          type: 'KAFKA',
          host: 'kafka-no-port.local',
        },
      ],
    });

    expect(getApiAccess(api)).toEqual(['kafka.local:9092', 'kafka-no-port.local']);
  });

  it('should return null for v4 native api when no kafka listeners', () => {
    const api = fakeNativeKafkaApiV4({ listeners: [] });

    expect(getApiAccess(api)).toEqual(null);
  });

  it('should return v4 tcp hosts when tcp listener exists', () => {
    const api = fakeApiV4({
      type: 'PROXY',
      listeners: [
        {
          type: 'TCP',
          entrypoints: [{ type: 'tcp-proxy' }],
          hosts: ['tcp-host-a', 'tcp-host-b'],
        },
      ],
    });

    expect(getApiAccess(api)).toEqual(['tcp-host-a', 'tcp-host-b']);
  });

  it('should return v4 http listener paths when no tcp hosts and http listener exists', () => {
    const api = fakeApiV4({
      type: 'PROXY',
      listeners: [
        {
          type: 'HTTP',
          entrypoints: [{ type: 'http-proxy' }],
          paths: [
            { host: 'example.com', path: '/p1' },
            { host: undefined, path: '/p2' },
          ],
        },
      ],
    });

    expect(getApiAccess(api)).toEqual(['example.com/p1', '/p2']);
  });

  it('should return null for v4 proxy when no tcp hosts and no http paths', () => {
    const api = fakeApiV4({ type: 'PROXY', listeners: [] });

    expect(getApiAccess(api)).toEqual(null);
  });
});
