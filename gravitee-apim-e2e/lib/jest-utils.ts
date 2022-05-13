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
import { expect } from '@jest/globals';
import { ApiResponse } from 'lib/management-webclient-sdk/src/lib/runtime';

interface PortalBusinessError {
  code: string;
  message: string;
  status: string;
  parameters: { [key: string]: string };
}

export async function fail(
  promise,
  expectedStatus: number,
  expectedError?: string | Partial<PortalBusinessError> | Array<Partial<PortalBusinessError>>,
) {
  try {
    await promise;
    throw new Error(`The test didn't fail as expected!`);
  } catch (error) {
    if (error.status == undefined) {
      throw error;
    }
    if (error.status !== expectedStatus) {
      console.debug(error);
    }
    expect(error.status).toEqual(expectedStatus);
    if (expectedError != null) {
      if (typeof expectedError === 'string') {
        const { message } = await error.json();
        expect(message).toEqual(expectedError);
      } else {
        const { errors } = await error.json();
        if (Array.isArray(expectedError)) {
          expect(errors).toEqual(expectedError);
        } else {
          expect(errors[0]).toEqual(expect.objectContaining(expectedError));
        }
      }
    }
  }
}

export async function unauthorized(promise: Promise<ApiResponse<any>>) {
  return fail(promise, 401);
}

export async function authorized(promise: Promise<ApiResponse<any>>, unexpectedStatus: number = 401) {
  try {
    const response = await promise;
    expect(response.raw.status).not.toEqual(unexpectedStatus);
  } catch (error) {
    if (error.status == undefined) {
      throw error;
    }
    expect(error.status).toBeDefined();
    expect(error.status).not.toEqual(unexpectedStatus);
  }
}

export async function forbidden(promise: Promise<ApiResponse<any>>) {
  return fail(promise, 403);
}

export async function notFound(
  promise: Promise<ApiResponse<any>>,
  expectedError?: string | Partial<PortalBusinessError> | Array<Partial<PortalBusinessError>>,
) {
  return fail(promise, 404);
}

export async function succeed(promise: Promise<ApiResponse<any>>, expectedStatus: number = 200) {
  const response = await promise;
  expect(response.raw.status).toEqual(expectedStatus);
  return await response.value();
}

export async function created(promise: Promise<ApiResponse<any>>) {
  return succeed(promise, 201);
}

export async function deleted(promise: Promise<ApiResponse<any>>) {
  return succeed(promise, 204);
}
