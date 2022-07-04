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

interface GatewayRequest {
  contextPath: string;
  expectedStatusCode: number;
  expectedResponseValidator: (response: Response) => boolean | Promise<boolean>; // Allows to validate if the expected request is the right one. Useful in case of api redeployment.
  method: HttpMethod;
  body?: string;
  headers?: HeadersInit;
  timeBetweenRetries: number;
  failAfterMs: number;
  timeout: number;
}

export async function fetchGatewaySuccess(request?: Partial<GatewayRequest>) {
  return _fetchGateway({ expectedStatusCode: 200, ...request });
}

export async function fetchGatewayUnauthorized(request?: Partial<GatewayRequest>) {
  return _fetchGateway({ expectedStatusCode: 401, ...request });
}

async function _fetchGateway(request?: Partial<GatewayRequest>): Promise<Response> {
  request = <GatewayRequest>{
    expectedStatusCode: 200,
    method: 'GET',
    timeBetweenRetries: 500,
    failAfterMs: 5000,
    timeout: 1500,
    ...request,
  };
  return new Promise((successCallback) => {
    setTimeout(() => {
      successCallback(_fetchGatewayWithRetries(request));
    }, request.timeout);
  });
}

async function _fetchGatewayWithRetries(request: Partial<GatewayRequest>): Promise<Response> {
  console.log('Try to fetch gateway', request.contextPath, request.failAfterMs);
  console.log('With headers', request.headers);
  const response = await fetchApi(`${process.env.GATEWAY_BASE_URL}${request.contextPath}`, {
    method: request.method,
    body: request.body,
    headers: request.headers,
  });

  const expectedResponseValidator = async () =>
    request.expectedResponseValidator ? await request.expectedResponseValidator(response) : true;

  // expect status first then expect validate response
  if (response.status != request.expectedStatusCode || !(await expectedResponseValidator())) {
    return new Promise((successCallback, failureCallback) => {
      setTimeout(() => {
        if (request.failAfterMs - request.timeBetweenRetries <= 0) {
          failureCallback(new Error(`Gateway [${process.env.GATEWAY_BASE_URL}${request.contextPath}] returned HTTP ${response.status}`));
        } else {
          request.failAfterMs -= request.timeBetweenRetries;
          successCallback(_fetchGateway(request));
        }
      }, request.timeBetweenRetries);
    });
  }
  return response;
}
