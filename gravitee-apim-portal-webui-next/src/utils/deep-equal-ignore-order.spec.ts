/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { deepEqualIgnoreOrder } from './deep-equal-ignore-order';

describe('deepEqualIgnoreOrder', () => {
  it('should return true for equal objects with properties in different order', () => {
    const obj1 = {
      a: 'apple',
      b: 'banana',
      c: 'cherry',
    };

    const obj2 = {
      c: 'cherry',
      a: 'apple',
      b: 'banana',
    };

    expect(deepEqualIgnoreOrder(obj1, obj2)).toBe(true);
  });

  it('should return false for objects with different properties', () => {
    const obj1 = {
      a: 'apple',
      b: 'banana',
    };

    const obj2 = {
      a: 'apple',
      c: 'cherry',
    };

    expect(deepEqualIgnoreOrder(obj1, obj2)).toBe(false);
  });

  it('should return true for objects with the same properties and values (nested)', () => {
    const obj1 = {
      a: 'apple',
      nested: {
        b: 'banana',
        c: 'cherry',
        d: ['potatoes', 'cucumbers'],
        e: [
          { f: 'tomatoes', g: 'salads' },
          { f: 'carrots', g: 'salads' },
        ],
      },
    };

    const obj2 = {
      a: 'apple',
      nested: {
        c: 'cherry',
        b: 'banana',
        d: ['potatoes', 'cucumbers'],
        e: [
          { g: 'salads', f: 'tomatoes' },
          { f: 'carrots', g: 'salads' },
        ],
      },
    };

    expect(deepEqualIgnoreOrder(obj1, obj2)).toBe(true);
  });

  it('should return false for objects with different nested values', () => {
    const obj1 = {
      a: 'apple',
      nested: {
        b: 'banana',
        c: 'cherry',
      },
    };

    const obj2 = {
      a: 'apple',
      nested: {
        b: 'blueberry',
        c: 'cherry',
      },
    };

    expect(deepEqualIgnoreOrder(obj1, obj2)).toBe(false);
  });

  it('should return true for identical arrays', () => {
    const arr1 = ['apple', 'banana', 'cherry'];
    const arr2 = ['apple', 'banana', 'cherry'];

    expect(deepEqualIgnoreOrder(arr1, arr2)).toBe(true);
  });

  it('should return true for arrays with elements in different order', () => {
    const arr1 = ['apple', 'banana', 'cherry'];
    const arr2 = ['banana', 'apple', 'cherry'];

    expect(deepEqualIgnoreOrder(arr1, arr2)).toBe(true);
  });

  it('should return false for arrays with different elements', () => {
    const arr1 = ['apple', 'banana'];
    const arr2 = ['apple', 'cherry'];

    expect(deepEqualIgnoreOrder(arr1, arr2)).toBe(false);
  });

  it('should return true for objects with identical nested arrays', () => {
    const obj1 = {
      fruits: ['apple', 'banana', 'cherry'],
    };

    const obj2 = {
      fruits: ['banana', 'cherry', 'apple'],
    };

    expect(deepEqualIgnoreOrder(obj1, obj2)).toBe(true);
  });

  it('should handle empty objects correctly', () => {
    const obj1 = {};
    const obj2 = {};

    expect(deepEqualIgnoreOrder(obj1, obj2)).toBe(true);
  });

  it('should return true for identical primitives', () => {
    const val1 = 42;
    const val2 = 42;

    expect(deepEqualIgnoreOrder(val1, val2)).toBe(true);
  });

  it('should return false for different primitive values', () => {
    const val1 = 42;
    const val2 = 43;

    expect(deepEqualIgnoreOrder(val1, val2)).toBe(false);
  });

  it('should return true for identical strings', () => {
    const str1 = 'hello';
    const str2 = 'hello';

    expect(deepEqualIgnoreOrder(str1, str2)).toBe(true);
  });

  it('should return false for different strings', () => {
    const str1 = 'hello';
    const str2 = 'world';

    expect(deepEqualIgnoreOrder(str1, str2)).toBe(false);
  });
});
