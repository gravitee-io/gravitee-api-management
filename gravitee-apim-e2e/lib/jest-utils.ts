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

export async function fail(promise, expectedStatus: number, expectedMessage?: string) {
  try {
    await promise;
    throw new Error(`The test didn't fail as expected!`);
  } catch (error) {
    expect(error.status).toEqual(expectedStatus);
    if (expectedMessage != null) {
      const { message } = await error.json();
      expect(message).toEqual(expectedMessage);
    }
  }
}

export async function unauthorized(promise: Promise<ApiResponse<any>>) {
  return fail(promise, 401);
}

export async function forbidden(promise: Promise<ApiResponse<any>>) {
  return fail(promise, 403);
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
