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
import http, { RefinedResponse } from 'k6/http';
import { sleep } from 'k6';
import { k6Options } from '@env/environment';

export class GatewayClient {
  /**
   * Retries calls to gateway until expected status is returned.
   * Default to:
   * expectedStatusCode: 200,
   * method: 'GET',
   * timeBetweenRetries: 1500,
   * maxRetries: 5,
   */
  static waitForApiAvailability(attributes: Partial<GatewayRequest>) {
    const request = <GatewayRequest>{
      expectedStatusCode: 200,
      method: 'GET',
      timeBetweenRetries: `${k6Options.apim.gatewaySyncInterval}`,
      maxRetries: 5,
      expectedResponseValidator: () => true,
      ...attributes,
    };

    //
    sleep(request.timeBetweenRetries / 1000);

    if (request.maxRetries <= 0) {
      return _fetchGateway(request);
    }

    let lastError: Error | undefined;

    for (let retries = request.maxRetries; retries > 0; --retries) {
      try {
        return _fetchGateway(request);
      } catch (error: any) {
        console.debug(error.toString());
        lastError = error;
        if (retries > 0) {
          console.info(`Retrying in ${request.timeBetweenRetries} ms with ${retries} attempts`);
          sleep(request.timeBetweenRetries / 1000);
        }
      }
    }

    console.error(
      `[${request.method}] [${k6Options.apim.gatewayBaseUrl}${request.contextPath}] failed after ${request.maxRetries} retries with error`,
      lastError,
    );

    throw lastError;
  }
}

function _fetchGateway(request: Partial<GatewayRequest>): RefinedResponse<any> {
  const response = http.request(request.method, `${k6Options.apim.gatewayBaseUrl}${request.contextPath}`, request.body, {
    tags: { name: OUT_OF_SCENARIO },
    headers: request.headers,
  });

  if (response.status != request.expectedStatusCode) {
    throw new Error(`[${request.method}] [${k6Options.apim.gatewayBaseUrl}${request.contextPath}] returned HTTP ${response.status}`);
  }

  const isValidResponse = request.expectedResponseValidator(response);

  if (!isValidResponse) {
    throw new Error(`Unexpected response for [${request.method}] [${k6Options.apim.gatewayBaseUrl}${request.contextPath}]`);
  }

  return response;
}

export enum HttpMethod {
  GET = 'GET',
  PUT = 'PUT',
  POST = 'POST',
  DELETE = 'DELETE',
  OPTIONS = 'OPTIONS',
}

interface GatewayRequest {
  contextPath: string;
  expectedStatusCode: number;
  expectedResponseValidator: (response: RefinedResponse<any>) => boolean | Promise<boolean>; // Allows to validate if the expected request is the right one. Useful in case of api redeployment.
  method: HttpMethod;
  body?: string;
  headers?: { [name: string]: string };
  timeBetweenRetries: number;
  maxRetries: number;
}

export const OUT_OF_SCENARIO = 'out-of-scenario';
