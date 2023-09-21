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

import { fetchEventSource } from './eventsource-fetch';
import { ApiResponse, ApisResponse } from '../management-v2-webclient-sdk/src/lib';
import { logger } from 'bs-logger';

export type HttpMethod = 'GET' | 'PUT' | 'POST' | 'DELETE' | 'OPTIONS';

interface BaseHttpRequest {
  headers?: HeadersInit;
  timeBetweenRetries: number;
  maxRetries: number;
  expectedStatusCode: number;
}

interface GatewayRequest extends BaseHttpRequest {
  contextPath: string;
  body?: string;
  method: HttpMethod;
  expectedResponseValidator: (response: Response) => boolean | Promise<boolean>; // Allows to validate if the expected request is the right one. Useful in case of api redeployment.
}

interface RestApiRequest<T> extends BaseHttpRequest {
  restApiHttpCall: () => Promise<ApiResponse<T>>;
  expectedResponseValidator: (response: RestApiResponseValidationParam<T>) => boolean | Promise<boolean>; // Allows to validate if the expected request is the right one. Useful in case of api redeployment.
}

interface RestApiResponseValidationParam<T> {
  status: number;
  headers: Headers;
  value: T;
}

export interface Logger {
  error(...data: any[]): void;
  info(...data: any[]): void;
}

export async function fetchRestApiSuccess<T>(request: Partial<RestApiRequest<T>>, logger: Logger = console): Promise<T> {
  return _fetchRestApiWithRetries({ expectedStatusCode: 200, ...request }, logger);
}

export async function fetchGatewaySuccess(request?: Partial<GatewayRequest>, logger: Logger = console) {
  return _fetchGatewayWithRetries({ expectedStatusCode: 200, ...request }, logger);
}

export async function fetchGatewayUnauthorized(request?: Partial<GatewayRequest>, logger: Logger = console) {
  return _fetchGatewayWithRetries({ expectedStatusCode: 401, ...request }, logger);
}

export async function fetchGatewayBadRequest(request?: Partial<GatewayRequest>, logger: Logger = console) {
  return _fetchGatewayWithRetries({ expectedStatusCode: 400, ...request }, logger);
}

export async function fetchGatewayServiceUnavailable(request?: Partial<GatewayRequest>, logger: Logger = console) {
  return _fetchGatewayWithRetries({ expectedStatusCode: 503, ...request }, logger);
}

async function _fetchGatewayWithRetries(attributes: Partial<GatewayRequest>, logger: Logger): Promise<Response> {
  const request = <GatewayRequest>{
    expectedStatusCode: 200,
    method: 'GET',
    timeBetweenRetries: 1500,
    maxRetries: 5,
    expectedResponseValidator: () => true,
    ...attributes,
  };

  if (request.maxRetries <= 0) {
    return await _fetchGateway(request);
  }

  let lastError: Error;

  for (let retries = request.maxRetries; retries > 0; --retries) {
    try {
      return await _fetchGateway(request);
    } catch (error) {
      // logger.info(error);
      lastError = error;
      if (retries > 0) {
        logger.info(`Retrying in ${request.timeBetweenRetries} ms with ${retries} attempts`);
        await sleep(request.timeBetweenRetries);
      }
    }
  }

  logger.info(
    `[${request.method}] [${process.env.GATEWAY_BASE_URL}${request.contextPath}] failed after ${request.maxRetries} retries with error`,
    lastError,
  );

  throw lastError;
}

async function _fetchRestApiWithRetries<T>(attributes: Partial<RestApiRequest<T>>, logger: Logger): Promise<T> {
  const request = <RestApiRequest<T>>{
    expectedStatusCode: 200,
    timeBetweenRetries: 1500,
    maxRetries: 5,
    expectedResponseValidator: () => true,
    ...attributes,
  };

  if (request.maxRetries <= 0) {
    return await _fetchRestApi(request);
  }

  let lastError: Error;

  for (let retries = request.maxRetries; retries > 0; --retries) {
    try {
      return await _fetchRestApi(request);
    } catch (error) {
      lastError = error;
      if (retries > 0) {
        logger.info(`Retrying in ${request.timeBetweenRetries} ms with ${retries} attempts`);
        await sleep(request.timeBetweenRetries);
      }
    }
  }

  logger.info(`[${request.restApiHttpCall.toString()}] failed after ${request.maxRetries} retries with error: \n`, lastError);

  throw lastError;
}

async function _fetchGateway(request: Partial<GatewayRequest>): Promise<Response> {
  const response = await fetchApi(`${process.env.GATEWAY_BASE_URL}${request.contextPath}`, {
    method: request.method,
    body: request.body,
    headers: request.headers,
  });

  if (response.status != request.expectedStatusCode) {
    throw new Error(`[${request.method}] [${process.env.GATEWAY_BASE_URL}${request.contextPath}] returned HTTP ${response.status}`);
  }

  const isValidResponse = await request.expectedResponseValidator(response);

  if (!isValidResponse) {
    throw new Error(`Unexpected response for [${request.method}] [${process.env.GATEWAY_BASE_URL}${request.contextPath}]`);
  }

  return response;
}

async function _fetchRestApi<T>(request: Partial<RestApiRequest<T>>): Promise<T> {
  const response: ApiResponse<T> = await request.restApiHttpCall();

  if (response.raw.status != request.expectedStatusCode) {
    throw new Error(`[${request.restApiHttpCall.toString()}] returned HTTP ${response.raw.status}`);
  }

  let responseObject: T = await response.value();
  let resolvedResponse = {
    value: responseObject,
    status: response.raw.status,
    headers: response.raw.headers,
  };
  const isValidResponse = await request.expectedResponseValidator(resolvedResponse);

  if (!isValidResponse) {
    throw new Error(`Unexpected response for [${request.restApiHttpCall.toString()}]: \n ${JSON.stringify(resolvedResponse, null, '\t')}`);
  }

  return responseObject;
}

export async function fetchEventSourceGateway(request: Partial<GatewayRequest>, onmessage, logger = console): Promise<unknown> {
  return await fetchEventSource(`${process.env.GATEWAY_BASE_URL}${request.contextPath}`, {
    onmessage,
    timeBetweenRetries: 1500,
    maxRetries: 5,
  });
}

export function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
