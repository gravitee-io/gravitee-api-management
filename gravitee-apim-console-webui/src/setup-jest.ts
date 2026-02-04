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

import { setupZoneTestEnv } from 'jest-preset-angular/setup-env/zone';
import 'chart.js/auto';
import 'jest-canvas-mock';

setupZoneTestEnv();

// mocking swagger-ui for tests as it contains some JS incompatible with Jest
// This means we will not be able to test Swagger in our components tests
jest.mock('swagger-ui', () => jest.fn());

// mocking asciidoctor for tests as it contains some JS incompatible with Jest
// This means we will not be able to test Asciidoctor in our components tests
jest.mock('@asciidoctor/core', () => jest.fn());

// mocking openapi-parser for tests
jest.mock('@scalar/openapi-parser', () => {
  const mockDereference = jest.fn().mockImplementation(async (spec) => ({
    schema: spec,
  }));
  const mockValidate = jest.fn().mockImplementation(async () => true);

  return {
    dereference: mockDereference,
    validate: mockValidate,
  };
});

// Mocking the Range object to avoid errors in tests (policy-studio-debug.component.spec.ts)
document.createRange = () => {
  const range = new Range();
  range.getBoundingClientRect = jest.fn();
  range.getClientRects = () => {
    return {
      item: () => null,
      length: 0,
      [Symbol.iterator]: jest.fn(),
    };
  };
  return range;
};

Object.defineProperty(window, 'CSS', { value: null });
Object.defineProperty(document, 'doctype', {
  value: '<!DOCTYPE html>',
});
Object.defineProperty(window, 'getComputedStyle', {
  value: () => {
    return {
      display: 'none',
      appearance: ['-webkit-appearance'],
      getPropertyValue: () => {
        return '';
      },
    };
  },
});
/**
 * ISSUE: https://github.com/angular/material2/issues/7101
 * Workaround for JSDOM missing transform property
 */
Object.defineProperty(document.body.style, 'transform', {
  value: () => {
    return {
      enumerable: true,
      configurable: true,
    };
  },
});

// Mock crypto.randomUUID for Jest environment. Used for default id generation in components.
Object.defineProperty(globalThis, 'crypto', {
  value: {
    ...globalThis.crypto,
    randomUUID: () =>
      'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.floor(Math.random() * 16);
        const v = c === 'x' ? r : (r % 4) + 8;
        return v.toString(16);
      }),
  },
});
