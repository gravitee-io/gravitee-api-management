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
import { APIRequestContext, APIResponse } from '@playwright/test';
import { NewPlanEntity } from '@model/plan';
import { BasicAuthentication } from '@model/users';
import { basicAuthHeader, DEFAULT_ORG_ENV_PATH } from './api-context';

export function createPlan(
  context: APIRequestContext,
  auth: BasicAuthentication,
  apiId: string,
  body: Partial<NewPlanEntity>,
): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}/plans`, { headers: basicAuthHeader(auth), data: body });
}

export function closePlan(context: APIRequestContext, auth: BasicAuthentication, apiId: string, planId: string): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}/plans/${planId}/_close`, { headers: basicAuthHeader(auth) });
}

export function publishPlan(context: APIRequestContext, auth: BasicAuthentication, apiId: string, planId: string): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}/plans/${planId}/_publish`, { headers: basicAuthHeader(auth) });
}
