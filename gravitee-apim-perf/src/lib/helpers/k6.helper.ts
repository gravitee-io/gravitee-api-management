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
import execution from 'k6/execution';

/**
 * Fails execution of k6 scenario if condition is fulfilled
 * @param condition
 * @param message
 */
export function failIf(condition: boolean, message: string = 'Test aborted') {
  if (condition) {
    execution.test.abort(message);
  }
}

export function generatePayloadInKB(expectedLength: number) {
  return generatePayloadInBytes(expectedLength * 1024);
}

export function generatePayloadInBytes(expectedLength: number) {
  let message: any = {};
  let i = 0;
  do {
    i = i + 1;
    message[`key_${i}`] = `value_${i}`;
  } while (JSON.stringify(message).length < expectedLength);
  return message;
}
