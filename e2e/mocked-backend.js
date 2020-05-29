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
exports.httpBackendMock = function () {
  angular.module('httpBackendMockModule', ['gravitee', 'ngMockE2E'])
    .run($httpBackend => {
      $httpBackend.whenGET('/management/apis/').respond([]);
      $httpBackend.whenGET('/management/user/').respond({
        username: 'MockedUser',
        authorities: [{
          authority: 'ADMIN'
        }]
      });
      $httpBackend.whenGET('/management/user/MockedUser/picture').respond('pic');
      $httpBackend.whenGET('/management/configuration/categories/').respond([]);

      $httpBackend.whenGET('/management/policies/').respond([{
        id: 'rate-limit',
        name: 'Rate Limiting',
        description: 'Description of the Rate Limit Gravitee Policy',
        version: '0.9.0'
      }]);

      $httpBackend.whenGET('/management/policies/rate-limit/schema').respond({
        type: 'object',
        id: 'urn:jsonschema:io:gravitee:policy:ratelimit:configuration:RateLimitPolicyConfiguration',
        properties: {
          async: {
            type: 'boolean',
            title: 'Non-strict mode'
          },
          rateLimits: {
            type: 'array',
            title: 'Rate Limits',
            items: {
              type: 'object',
              id: 'urn:jsonschema:io:gravitee:policy:ratelimit:configuration:RateLimitConfiguration',
              title: 'Rate Limit',
              properties: {
                limit: {
                  title: 'Max requests',
                  type: 'integer'
                },
                periodTime: {
                  title: 'Time period',
                  type: 'integer'
                },
                periodTimeUnit: {
                  title: 'Time unit',
                  type: 'string',
                  enum: [
                    'SECONDS',
                    'MINUTES',
                    'HOURS',
                    'DAYS'
                  ]
                }
              },
              required: [
                'limit',
                'periodTime',
                'periodTimeUnit'
              ]
            }
          }
        },
        required: [
          'rateLimits'
        ]
      });

      $httpBackend.whenGET('/management/apis/swapi/state').respond({
        api_id: 'swapi',
        is_synchronized: true
      });

      $httpBackend.whenGET('/management/apis/swapi').respond({
        id: 'swapi',
        name: 'SWAPI',
        version: '1.0',
        description: 'The Star Wars API.',
        visibility: 'private',
        state: 'started',
        permission: 'primary_owner',
        tags: [],
        proxy: {
          context_path: '/swapi',
          endpoints: [{
            target: 'http://swapi.co/api',
            weight: 1,
            backup: false,
            healthcheck: true
          }, {
            target: 'http://docs.gravitee.io/',
            weight: 1,
            backup: false,
            healthcheck: true
          }],
          load_balancing: {
            type: 'ROUND_ROBIN'
          },
          failover: {
            maxAttempts: 1,
            retryTimeout: 0,
            cases: ['TIMEOUT']
          },
          strip_context_path: false,
          http: {
            http_proxy: {
              enabled: false,
              host: 'null',
              port: 0,
              type: 'HTTP'
            },
            configuration: {
              connectTimeout: 5000,
              idleTimeout: 60000,
              keepAlive: true,
              dumpRequest: false,
              readTimeout: 10000,
              pipelining: false,
              maxConcurrentConnections: 100,
              useCompression: false
            }
          }
        },
        paths: {
          '/': [{
            methods: ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS', 'TRACE', 'CONNECT'],
            'rate-limit': {async: null, rateLimits: []}
          }]
        },
        deployed_at: 1466586262857,
        created_at: 1466193438830,
        updated_at: 1470209016229,
        owner: {
          username: 'admin'
        },
        services: {
          'health-check': {
            interval: 4,
            unit: 'SECONDS',
            enabled: true,
            request: {
              uri: '/',
              method: 'GET',
              headers: [{
                name: 'Accept',
                value: 'application/json'
              }]
            },
            expectation: {
              assertions: ['#response.status == 200']
            }
          }
        },
        resources: []
      });

      $httpBackend.whenGET(/.*/).passThrough();
    });
};
