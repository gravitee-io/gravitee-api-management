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
import { expect, test } from '@jest/globals';
import { ApiResponse, ResponseError } from '@gravitee/management-webclient-sdk/src/lib/runtime';

interface PortalBusinessError {
  code: string;
  message: string;
  status: string;
  parameters: Record<string, string>;
}

export async function fail(
  promise,
  expectedStatus: number,
  expectedError?: string | RegExp | Partial<PortalBusinessError> | Array<Partial<PortalBusinessError>>,
) {
  try {
    await promise;
    throw new Error(`The test didn't fail as expected!`);
  } catch (error) {
    const response = error.response ? error.response : error;
    if (response == undefined || response.status == undefined) {
      throw response;
    }
    if (response.status !== expectedStatus) {
      console.debug(response);
    }
    expect(response.status).toEqual(expectedStatus);
    if (expectedError != null) {
      if (typeof expectedError === 'string') {
        const { message } = await response.json();
        expect(message).toEqual(expectedError);
      } else if (expectedError instanceof RegExp) {
        const { message } = await response.json();
        expect(message).toMatch(expectedError);
      } else {
        const { errors } = await response.json();
        if (Array.isArray(expectedError)) {
          expect(errors).toHaveLength(expectedError.length);
          expect(errors).toEqual(expect.arrayContaining(expectedError.map((expectedError) => expect.objectContaining(expectedError))));
        } else {
          expect(errors[0]).toEqual(expect.objectContaining(expectedError));
        }
      }
    }
  }
}

export async function unauthorized<T>(promise: Promise<ApiResponse<T>>): Promise<void> {
  return fail(promise, 401);
}

export async function authorized<T>(promise: Promise<ApiResponse<T>>, unexpectedStatus: number = 401): Promise<void> {
  try {
    const response = await promise;
    expect(response.raw.status).not.toEqual(unexpectedStatus);
  } catch (error) {
    const response = error.response ? error.response : error;
    if (response == undefined || response.status == undefined) {
      throw response;
    }
    expect(response.status).toBeDefined();
    expect(response.status).not.toEqual(unexpectedStatus);
  }
}

export async function forbidden<T>(promise: Promise<ApiResponse<T>>): Promise<void> {
  return fail(promise, 403);
}

export async function notFound<T>(
  promise: Promise<ApiResponse<T>>,
  expectedError?: string | Partial<PortalBusinessError> | Array<Partial<PortalBusinessError>>,
) {
  return fail(promise, 404, expectedError);
}

export async function succeed<T>(promise: Promise<ApiResponse<T>>, expectedStatus: number = 200): Promise<T> {
  try {
    const response = await promise;
    expect(response.raw.status).toEqual(expectedStatus);
    return await response.value();
  } catch (error) {
    const response = error.response ? error.response : error;
    if (response == undefined || response.status == undefined) {
      throw response;
    }
    // improve ResponseError message with response status
    let responseError = error as ResponseError;
    let errorContent = await responseError.response.json();
    let errorMessage = errorContent.message;
    let errorParameters = JSON.stringify(errorContent.parameters);
    throw new ResponseError(
      responseError.response,
      `${expect.getState().currentTestName}\n\n Response returned an error code: ${
        responseError.response.status
      }\n Message: ${errorMessage}\n Parameters: ${errorParameters}`,
    );
  }
}

export async function created<T>(promise: Promise<ApiResponse<T>>) {
  return succeed(promise, 201);
}

export async function noContent<T>(promise: Promise<ApiResponse<T>>) {
  return succeed(promise, 204);
}

export const testif = (condition) => (condition ? test : test.skip);

export const describeIf = (condition) => (condition ? describe : describe.skip);

export const describeIfV3 = describeIf(process.env.V4_EMULATION_ENGINE_DEFAULT == 'no');
export const describeIfV4EmulationEngine = describeIf(process.env.V4_EMULATION_ENGINE_DEFAULT == 'yes');

export * from './jest-retry';
