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
import { UpdateApiEntity } from '@model/apis';
import { ApiImport, ImportSwaggerDescriptorEntity } from '@model/api-imports';
import { BasicAuthentication } from '@model/users';
import { basicAuthHeader, DEFAULT_ORG_ENV_PATH } from './api-context';

export function deleteApi(context: APIRequestContext, auth: BasicAuthentication, apiId: string): Promise<APIResponse> {
  return context.delete(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}`, { headers: basicAuthHeader(auth) });
}

export function deleteV4Api(
  context: APIRequestContext,
  auth: BasicAuthentication,
  apiId: string,
  closePlans: boolean,
): Promise<APIResponse> {
  return context.delete(`/management/v2/environments/DEFAULT/apis/${apiId}`, {
    headers: basicAuthHeader(auth),
    params: { closePlans: String(closePlans) },
  });
}

export function deployApi(context: APIRequestContext, auth: BasicAuthentication, apiId: string): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}/deploy`, { headers: basicAuthHeader(auth) });
}

export function startApi(context: APIRequestContext, auth: BasicAuthentication, apiId: string): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}`, { headers: basicAuthHeader(auth), params: { action: 'START' } });
}

export function stopApi(context: APIRequestContext, auth: BasicAuthentication, apiId: string): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}`, { headers: basicAuthHeader(auth), params: { action: 'STOP' } });
}

export function stopV4Api(context: APIRequestContext, auth: BasicAuthentication, apiId: string): Promise<APIResponse> {
  return context.post(`/management/v2/environments/DEFAULT/apis/${apiId}/_stop`, { headers: basicAuthHeader(auth) });
}

export function importCreateApi(context: APIRequestContext, auth: BasicAuthentication, body: ApiImport): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/import`, { headers: basicAuthHeader(auth), data: body });
}

export function importSwaggerApi(
  context: APIRequestContext,
  auth: BasicAuthentication,
  swaggerImport: string,
  attributes?: Partial<ImportSwaggerDescriptorEntity>,
): Promise<APIResponse> {
  return context.post(`${DEFAULT_ORG_ENV_PATH}/apis/import/swagger`, {
    headers: basicAuthHeader(auth),
    params: { definitionVersion: '2.0.0' },
    data: { payload: swaggerImport, ...attributes },
  });
}

export function updateApi(
  context: APIRequestContext,
  auth: BasicAuthentication,
  apiId: string,
  apiUpdate: UpdateApiEntity,
): Promise<APIResponse> {
  return context.put(`${DEFAULT_ORG_ENV_PATH}/apis/${apiId}`, { headers: basicAuthHeader(auth), data: apiUpdate });
}
