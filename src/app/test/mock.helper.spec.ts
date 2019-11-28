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
import { Provider, Type } from '@angular/core';

export type Mock<T> = T & { [P in keyof T]: T[P] & jasmine.Spy };

export function createMock<T>(type: Type<T>): Mock<T> {
  const target: any = {};

  function installProtoMethods(proto: any) {
    if (proto === null || proto === Object.prototype) {
      return;
    }

    for (const key of Object.getOwnPropertyNames(proto)) {
      // tslint:disable-next-line: no-non-null-assertion
      const descriptor = Object.getOwnPropertyDescriptor(proto, key)!;

      if (typeof descriptor.value === 'function' && key !== 'constructor') {
        target[key] = jasmine.createSpy(key);
      }
    }

    installProtoMethods(Object.getPrototypeOf(proto));
  }
  installProtoMethods(type.prototype);
  return target;
}

export function provideMock<T>(type: Type<T>): Provider {
  return { provide: type, useFactory: () => createMock(type) };
}
