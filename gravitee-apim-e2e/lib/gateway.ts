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
import 'dotenv/config';
import fetchApi, { HeadersInit, Response } from 'node-fetch';

export type HttpMethod = 'GET' | 'PUT' | 'POST' | 'DELETE';

export async function fetchGateway(
  contextPath: string,
  method: HttpMethod = 'GET',
  body?: string,
  headers?: HeadersInit,
  timeBetweenRetries = 500,
  failAfterMs = 5000,
  timeout = 1500,
): Promise<Response> {
  return new Promise((successCallback) => {
    setTimeout(() => {
      successCallback(_fetchGateway(contextPath, method, timeBetweenRetries, failAfterMs, body, headers));
    }, timeout);
  });
}

async function _fetchGateway(
  contextPath: string,
  method: HttpMethod = 'GET',
  timeBetweenRetries: number,
  failAfterMs: number,
  body?: string,
  headers?: HeadersInit,
): Promise<Response> {
  console.log('Try to fetch gateway', contextPath, failAfterMs);
  const response = await fetchApi(`${process.env.GATEWAY_BASE_PATH}${contextPath}`, {
    method,
    body,
    headers,
  });
  if (response.status == 404) {
    return new Promise((successCallback, failureCallback) => {
      setTimeout(() => {
        if (failAfterMs - timeBetweenRetries <= 0) {
          failureCallback(new Error(`Gateway [${process.env.GATEWAY_BASE_PATH}${contextPath}] not found`));
        } else {
          failAfterMs -= timeBetweenRetries;
          successCallback(_fetchGateway(contextPath, method, timeBetweenRetries, failAfterMs, body, headers));
        }
      }, timeBetweenRetries);
    });
  }
  return response;
}
