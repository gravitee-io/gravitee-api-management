'use strict';
Object.defineProperty(exports, '__esModule', { value: true });
exports.fetchGatewayBadRequest = exports.fetchGatewayUnauthorized = exports.fetchGatewaySuccess = void 0;
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
require('dotenv/config');
const node_fetch_1 = require('node-fetch');
async function fetchGatewaySuccess(request) {
  return _fetchGatewayWithRetries({ expectedStatusCode: 200, ...request });
}
exports.fetchGatewaySuccess = fetchGatewaySuccess;
async function fetchGatewayUnauthorized(request) {
  return _fetchGatewayWithRetries({ expectedStatusCode: 401, ...request });
}
exports.fetchGatewayUnauthorized = fetchGatewayUnauthorized;
async function fetchGatewayBadRequest(request) {
  return _fetchGatewayWithRetries({ expectedStatusCode: 400, ...request });
}
exports.fetchGatewayBadRequest = fetchGatewayBadRequest;
async function _fetchGatewayWithRetries(attributes) {
  const request = {
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
  let lastError;
  for (let retries = request.maxRetries; retries > 0; --retries) {
    try {
      return await _fetchGateway(request);
    } catch (error) {
      lastError = error;
      if (retries > 0) {
        console.info(`Retrying in ${request.timeBetweenRetries} ms with ${retries} attempts`);
        await sleep(request.timeBetweenRetries);
      }
    }
  }
  console.info(
    `[${request.method}] [${process.env.GATEWAY_BASE_URL}${request.contextPath}] failed after ${request.maxRetries} retries with error`,
    lastError,
  );
  throw lastError;
}
async function _fetchGateway(request) {
  const response = await (0, node_fetch_1.default)(`${process.env.GATEWAY_BASE_URL}${request.contextPath}`, {
    method: request.method,
    body: request.body,
    headers: request.headers,
  });
  const isValidResponse = await request.expectedResponseValidator(response);
  if (!isValidResponse) {
    throw new Error(`Unexpected response for [${request.method}] [${process.env.GATEWAY_BASE_URL}${request.contextPath}]`);
  }
  if (response.status != request.expectedStatusCode) {
    throw new Error(`[${request.method}] [${process.env.GATEWAY_BASE_URL}${request.contextPath}] returned HTTP ${response.status}`);
  }
  return response;
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
