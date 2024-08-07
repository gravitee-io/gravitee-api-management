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
import { TestBed } from '@angular/core/testing';
import { ResolveFn } from '@angular/router';

import { applicationResolver, applicationTypeResolver } from './application.resolver';
import { Application, ApplicationType } from '../entities/application/application';

describe('applicationResolver', () => {
  const executeResolver: ResolveFn<Application> = (...resolverParameters) =>
    TestBed.runInInjectionContext(() => applicationResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});

describe('applicationTypeResolver', () => {
  const executeResolver: ResolveFn<ApplicationType> = (...resolverParameters) =>
    TestBed.runInInjectionContext(() => applicationTypeResolver(...resolverParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeResolver).toBeTruthy();
  });
});
