/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

import { MonacoSuggestionsService } from './monaco-suggestions.service';

describe('MonacoSuggestionsService', () => {
  let service: MonacoSuggestionsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MonacoSuggestionsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should register suggestions without errors', () => {
    // Mock Monaco object
    const mockMonaco = {
      languages: {
        registerCompletionItemProvider: jest.fn().mockReturnValue({ dispose: jest.fn() }),
        CompletionItemKind: {
          Snippet: 25,
        },
        CompletionItemInsertTextRule: {
          InsertAsSnippet: 4,
        },
      },
    };

    expect(() => {
      service.registerSuggestions(mockMonaco as any);
    }).not.toThrow();
  });

  it('should dispose without errors', () => {
    expect(() => {
      service.dispose();
    }).not.toThrow();
  });
});
