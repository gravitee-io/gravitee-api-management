import { Provider, Type } from '@angular/core';

export type Mock<T> = T & { [P in keyof T]: T[P] & jasmine.Spy };

export function createMagicalMock<T>(type: Type<T>): Mock<T> {
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

export function provideMagicalMock<T>(type: Type<T>): Provider {
  return {
    provide: type,
    useFactory: () => createMagicalMock(type)
  };
}
