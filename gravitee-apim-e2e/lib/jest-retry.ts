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
import { test } from '@jest/globals';
import { AsyncFn } from '@jest/types/build/Circus';

function runTest(handler) {
  return new Promise((resolve, reject) => {
    const result = handler((err) => (err ? reject(err) : resolve({})));

    if (result && result.then) {
      result.catch(reject).then(resolve);
    } else {
      resolve({});
    }
  });
}

export function flakyRunner(retries = 3, delayMs = 1000) {
  return {
    async test(description: string, handler: AsyncFn) {
      test(description, async () => {
        let latestError;
        for (let tries = 0; tries < retries; tries++) {
          try {
            await runTest(handler);
            return;
          } catch (error) {
            latestError = error;
            console.info('Test failed, available retries:', retries - tries);
            await sleep(delayMs);
          }
        }

        throw latestError;
      });
    },
  };
}

export async function sleep(time: number) {
  return new Promise((resolve) => setTimeout(resolve, time));
}
