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
import { fieldIsSet, fieldSet, fieldUnSet } from './bitfield';

describe('Bitfield', () => {
  it('set a bit', () => {
    const emptyField = 0;

    const result = fieldSet(emptyField, 1);

    expect(fieldIsSet(result, 1)).toBeTruthy();
  });

  it('verify bit set', () => {
    const emptyField = 0;

    const result = fieldIsSet(emptyField, 1);

    expect(result).toBeFalsy();
  });

  it('set multiple bits', () => {
    const settedField = [1, 2, 4].reduce((field, value) => fieldSet(field, value), 0);

    expect(fieldIsSet(settedField, 1)).toBeTruthy();
    expect(fieldIsSet(settedField, 2)).toBeTruthy();
    expect(fieldIsSet(settedField, 4)).toBeTruthy();
    expect(fieldIsSet(settedField, 8)).toBeFalsy();
  });

  it('unset a bits', () => {
    let field = [1, 2, 4].reduce((field, value) => fieldSet(field, value), 0);

    field = fieldUnSet(field, 2);

    expect(fieldIsSet(field, 1)).toBeTruthy();
    expect(fieldIsSet(field, 2)).toBeFalsy();
    expect(fieldIsSet(field, 4)).toBeTruthy();
  });
});
