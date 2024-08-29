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
import { Header } from '@gravitee/ui-particles-angular';
import { FormControl } from '@angular/forms';

import { toGioFormHeader, toDictionary, uniqueKeysValidator } from './gio-form-header.util';

describe('toGioFormHeader', () => {
  it('should return an empty array when the input is undefined', () => {
    const result = toGioFormHeader(undefined);
    expect(result).toEqual([]);
  });

  it('should return an empty array when the input is an empty object', () => {
    const result = toGioFormHeader({});
    expect(result).toEqual([]);
  });

  it('should convert a non-empty object to an array of headers', () => {
    const input = {
      'Content-Type': 'application/json',
      Authorization: 'Bearer token',
    };
    const expectedOutput: Header[] = [
      { key: 'Content-Type', value: 'application/json' },
      { key: 'Authorization', value: 'Bearer token' },
    ];
    const result = toGioFormHeader(input);
    expect(result).toEqual(expectedOutput);
  });
});

describe('toDictionary', () => {
  it('should return an empty object when the input is undefined', () => {
    const result = toDictionary(undefined);
    expect(result).toEqual({});
  });

  it('should return an empty object when the input is null', () => {
    const result = toDictionary(null);
    expect(result).toEqual({});
  });

  it('should return an empty object when the input is an empty array', () => {
    const result = toDictionary([]);
    expect(result).toEqual({});
  });

  it('should convert a non-empty array to a dictionary', () => {
    const input: Header[] = [
      { key: 'Content-Type', value: 'application/json' },
      { key: 'Authorization', value: 'Bearer token' },
    ];
    const expectedOutput = {
      'Content-Type': 'application/json',
      Authorization: 'Bearer token',
    };
    const result = toDictionary(input);
    expect(result).toEqual(expectedOutput);
  });
});

describe('uniqueKeysValidator', () => {
  it('should return null if there are no headers', () => {
    const control = new FormControl([]);
    const result = uniqueKeysValidator()(control);
    expect(result).toBeNull();
  });

  it('should return null if all keys are unique', () => {
    const headers: Header[] = [
      { key: 'key1', value: 'value1' },
      { key: 'key2', value: 'value2' },
    ];
    const control = new FormControl(headers);
    const result = uniqueKeysValidator()(control);
    expect(result).toBeNull();
  });

  it('should return an error object if there are duplicate keys', () => {
    const headers: Header[] = [
      { key: 'key1', value: 'value1' },
      { key: 'key1', value: 'value2' },
    ];
    const control = new FormControl(headers);
    const result = uniqueKeysValidator()(control);
    expect(result).toEqual({ nonUniqueKeys: true });
  });
});
