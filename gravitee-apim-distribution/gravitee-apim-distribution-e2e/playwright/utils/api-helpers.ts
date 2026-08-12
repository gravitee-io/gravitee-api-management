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
import { Api } from '@model/apis';
import { ApiImport } from '@model/api-imports';
import { ADMIN_USER } from '@test-data/fakers/users';
import { GATEWAY_URL } from './api-context';
import { closePlan } from './api-plan-management-commands';
import { deleteApi, deleteV4Api, stopApi, stopV4Api } from './api-management-commands';
import { expectNoContent, expectOk } from './api-assertions';

export async function teardownApi(context: APIRequestContext, api: ApiImport | Api): Promise<void> {
  const plans = api.plans ?? [];
  for (const plan of plans) {
    expectOk(await closePlan(context, ADMIN_USER, api.id, plan.id));
  }
  await stopApi(context, ADMIN_USER, api.id);
  expectNoContent(await deleteApi(context, ADMIN_USER, api.id));
}

export async function teardownV4Api(context: APIRequestContext, apiId: string): Promise<void> {
  await stopV4Api(context, ADMIN_USER, apiId);
  expectNoContent(await deleteV4Api(context, ADMIN_USER, apiId, true));
}

export async function callGateway(
  context: APIRequestContext,
  contextPath: string,
  checkConditionFn: (response: APIResponse) => boolean | Promise<boolean> = (response) => response.status() === 200,
  maxRetries = 20,
  retryDelay = 1500,
): Promise<APIResponse> {
  const url = `${GATEWAY_URL}${contextPath}`;
  for (let retriesLeft = maxRetries; ; retriesLeft--) {
    const response = await context.get(url);
    if (await checkConditionFn(response)) {
      return response;
    }
    if (retriesLeft <= 0) {
      throw new Error(
        `API did not return the expected result within the allowed retries.\nFunction used to check the response:\n${checkConditionFn.toString()}`,
      );
    }
    await new Promise((resolve) => setTimeout(resolve, retryDelay));
  }
}
