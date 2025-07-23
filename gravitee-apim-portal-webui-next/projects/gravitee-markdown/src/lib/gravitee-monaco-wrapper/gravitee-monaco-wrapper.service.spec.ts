/*
 * Copyright (C) 2023 The Gravitee team (http://gravitee.io)
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

import { GRAVITEE_MONACO_EDITOR_CONFIG } from './data/gravitee-monaco-editor-config';
import { GraviteeMonacoWrapperService } from './gravitee-monaco-wrapper.service';

describe('GraviteeMonacoWrapperService', () => {
  let service: GraviteeMonacoWrapperService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        GraviteeMonacoWrapperService,
        {
          provide: GRAVITEE_MONACO_EDITOR_CONFIG,
          useValue: {
            baseUrl: 'http://localhost:3000',
          },
        },
      ],
    });
    service = TestBed.inject(GraviteeMonacoWrapperService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have loaded$ observable', () => {
    expect(service.loaded$).toBeDefined();
  });

  it('should have loadEditor method', () => {
    expect(service.loadEditor).toBeDefined();
    expect(typeof service.loadEditor).toBe('function');
  });

  it('should return a promise from loadEditor', () => {
    const result = service.loadEditor();
    expect(result).toBeInstanceOf(Promise);
  });

  it('should handle multiple loadEditor calls', () => {
    const promise1 = service.loadEditor();
    const promise2 = service.loadEditor();

    expect(promise1).toBeInstanceOf(Promise);
    expect(promise2).toBeInstanceOf(Promise);

    // Both should be promises
    expect(typeof promise1.then).toBe('function');
    expect(typeof promise2.then).toBe('function');
  });

  it('should have proper configuration injection', () => {
    // Test that the service can be created with configuration
    expect(service).toBeTruthy();
  });
});
