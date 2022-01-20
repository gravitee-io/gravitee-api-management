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

import * as _ from 'lodash';

import { ApiService } from '../../services/api.service';
import ApplicationService from '../../services/application.service';
import TenantService from '../../services/tenant.service';
import { CompareCondition, Metrics, Scope, StringCondition, ThresholdCondition, ThresholdRangeCondition, Tuple } from '../alert';

export class ApiMetrics extends Metrics {
  static RESPONSE_TIME: ApiMetrics = new ApiMetrics('response.response_time', 'Response Time (ms)', [
    ThresholdCondition.TYPE,
    ThresholdRangeCondition.TYPE,
    CompareCondition.TYPE,
  ]);

  static QUOTA_COUNTER: ApiMetrics = new ApiMetrics('quota.counter', 'Quota Counter', [CompareCondition.TYPE]);

  static UPSTREAM_RESPONSE_TIME: ApiMetrics = new ApiMetrics('response.upstream_response_time', 'Upstream Response Time (ms)', [
    ThresholdCondition.TYPE,
    ThresholdRangeCondition.TYPE,
    CompareCondition.TYPE,
  ]);

  static RESPONSE_STATUS: ApiMetrics = new ApiMetrics(
    'response.status',
    'Status Code',
    [ThresholdCondition.TYPE, ThresholdRangeCondition.TYPE],
    true,
  );

  static ERROR_KEY: ApiMetrics = new ApiMetrics('error.key', 'Error Key', [StringCondition.TYPE], true, undefined, () => {
    const keys: Tuple[] = [];
    [
      'API_KEY_MISSING',
      'API_KEY_INVALID',
      'QUOTA_TOO_MANY_REQUESTS',
      'RATE_LIMIT_TOO_MANY_REQUESTS',
      'REQUEST_CONTENT_LIMIT_TOO_LARGE',
      'REQUEST_CONTENT_LIMIT_LENGTH_REQUIRED',
      'REQUEST_TIMEOUT',
      'REQUEST_VALIDATION_INVALID',
      'RESOURCE_FILTERING_FORBIDDEN',
      'RESOURCE_FILTERING_METHOD_NOT_ALLOWED',
      'RBAC_FORBIDDEN',
      'RBAC_INVALID_USER_ROLES',
      'RBAC_NO_USER_ROLE',
      'OAUTH2_MISSING_SERVER',
      'OAUTH2_MISSING_HEADER',
      'OAUTH2_MISSING_ACCESS_TOKEN',
      'OAUTH2_INVALID_ACCESS_TOKEN',
      'OAUTH2_INVALID_SERVER_RESPONSE',
      'OAUTH2_INSUFFICIENT_SCOPE',
      'OAUTH2_SERVER_UNAVAILABLE',
      'HTTP_SIGNATURE_INVALID_SIGNATURE',
      'JWT_MISSING_TOKEN',
      'JWT_INVALID_TOKEN',
      'JSON_INVALID_PAYLOAD',
      'JSON_INVALID_FORMAT',
      'GATEWAY_INVALID_REQUEST',
      'GATEWAY_INVALID_RESPONSE',
      'GATEWAY_OAUTH2_ACCESS_DENIED',
      'GATEWAY_OAUTH2_SERVER_ERROR',
      'GATEWAY_OAUTH2_INVALID_CLIENT',
      'GATEWAY_MISSING_SECURITY_PROVIDER',
      'GATEWAY_POLICY_INTERNAL_ERROR',
      'GATEWAY_MISSING_SECURED_REQUEST_PLAN',
      'GATEWAY_CLIENT_CONNECTION_ERROR',
      'GATEWAY_CLIENT_CONNECTION_TIMEOUT',
    ].forEach((key) => {
      keys.push(new Tuple(key, key));
    });

    return keys;
  });

  static REQUEST_CONTENT_LENGTH: ApiMetrics = new ApiMetrics('request.content_length', 'Request Content-Length', [
    ThresholdCondition.TYPE,
    ThresholdRangeCondition.TYPE,
    CompareCondition.TYPE,
  ]);

  static RESPONSE_CONTENT_LENGTH: ApiMetrics = new ApiMetrics('response.content_length', 'Response Content-Length', [
    ThresholdCondition.TYPE,
    ThresholdRangeCondition.TYPE,
    CompareCondition.TYPE,
  ]);

  static TENANT: ApiMetrics = new ApiMetrics(
    'tenant',
    'Tenant',
    [StringCondition.TYPE],
    true,
    undefined,
    (type: number, id: string, $injector: any) => {
      const tenants: Tuple[] = [];

      // PLATFORM: Search for all registered tenants
      ($injector.get('TenantService') as TenantService).list().then((result) => {
        result.data.forEach((tenant) => {
          tenants.push(new Tuple(tenant.id, tenant.name));
        });
      });

      return tenants;
    },
  );

  static API: ApiMetrics = new ApiMetrics(
    'api',
    'API',
    [StringCondition.TYPE],
    true,
    [Scope.APPLICATION, Scope.PLATFORM],
    (type: number, id: string, $injector: any) => {
      const apis: Tuple[] = [];

      if (type === 2) {
        // PLATFORM: Search for all registered APIs
        ($injector.get('ApiService') as ApiService).searchApis().then((result) => {
          result.data.forEach((api) => {
            apis.push(new Tuple(api.id, api.name));
          });
        });
      } else if (type === 1) {
        // APPLICATION: Search for all subscribed APIs
        ($injector.get('ApplicationService') as ApplicationService).getSubscribedAPI(id).then((result) => {
          result.data.forEach((api) => {
            apis.push(new Tuple(api.id, api.name));
          });
        });
      }

      return apis;
    },
  );

  static APPLICATION: ApiMetrics = new ApiMetrics(
    'application',
    'Application',
    [StringCondition.TYPE],
    true,
    [Scope.API, Scope.PLATFORM],
    (type: number, id: string, $injector: any) => {
      const applications: Tuple[] = [];

      if (type === 0) {
        // API: Search for all subscribed applications
        ($injector.get('ApiService') as ApiService).getSubscribers(id).then((result) => {
          result.data.forEach((application) => {
            applications.push(new Tuple(application.id, application.name));
          });
        });
      }

      return applications;
    },
  );

  static PLAN: ApiMetrics = new ApiMetrics(
    'plan',
    'Plan',
    [StringCondition.TYPE],
    true,
    undefined,
    (type: number, id: string, $injector: any) => {
      const plans: Tuple[] = [];

      if (type === 0) {
        // API: Search for all published plans
        ($injector.get('ApiService') as ApiService).getPublishedApiPlans(id).then((result) => {
          result.data.forEach((plan) => {
            plans.push(new Tuple(plan.id, plan.name));
          });
        });
      } else if (type === 1) {
        // APPLICATION: Search for all subscribed plan
        ($injector.get('ApplicationService') as ApplicationService).listSubscriptions(id).then((result) => {
          const keyPlans = _.keys(_.keyBy(result.data.data, (sub) => sub.plan));
          _.each(keyPlans, (plan) => {
            plans.push(new Tuple(plan, result.data.metadata[plan].name));
          });
        });
      }

      return plans;
    },
  );

  static METRICS: ApiMetrics[] = [
    ApiMetrics.RESPONSE_TIME,
    ApiMetrics.UPSTREAM_RESPONSE_TIME,
    ApiMetrics.RESPONSE_STATUS,
    ApiMetrics.REQUEST_CONTENT_LENGTH,
    ApiMetrics.RESPONSE_CONTENT_LENGTH,
    ApiMetrics.ERROR_KEY,
    ApiMetrics.API,
    ApiMetrics.TENANT,
    ApiMetrics.APPLICATION,
    ApiMetrics.PLAN,
  ];
}
