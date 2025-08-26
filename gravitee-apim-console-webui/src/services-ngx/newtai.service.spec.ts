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
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ElAiPromptState } from '@gravitee/ui-particles-angular';
import { provideHttpClient } from '@angular/common/http';

import { NewtAIService } from './newtai.service';

import { Constants } from '../entities/Constants';

describe('NewtAIService', () => {
  let service: NewtAIService;
  let httpTestingController: HttpTestingController;
  const mockConstants = {
    env: {
      v2BaseURL: 'https://api.gravitee.io/management/v2',
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [provideHttpClient(), provideHttpClientTesting(), NewtAIService, { provide: Constants, useValue: mockConstants }],
    });

    service = TestBed.inject(NewtAIService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('promptEL', () => {
    it('should make a POST request and return mapped ElAiPromptState', (done) => {
      // Given
      const prompt = 'Generate EL expression for request filtering';
      const apiId = 'test-api-id';
      const mockResponse = {
        message: "{ #request.headers['x-api-key'] == 'valid-key' }",
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      };
      const expectedResult: ElAiPromptState = {
        el: "{ #request.headers['x-api-key'] == 'valid-key' }",
      };
      service.addToContext('apiId', apiId);

      // When
      service.promptEL(prompt).subscribe((result) => {
        // Then
        expect(result).toEqual(expectedResult);
        done();
      });

      const req = httpTestingController.expectOne(`${mockConstants.env.v2BaseURL}/newtai/el/_generate`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ message: prompt, context: { apiId } });

      req.flush(mockResponse);
    });

    it('should propagate error when the API call fails', (done) => {
      // Given
      const prompt = 'Generate EL expression';
      const apiId = 'test-api-id';
      const errorResponse = {
        status: 'error',
        message: 'Failed to generate EL expression',
        code: 'AI_SERVICE_ERROR',
        parameters: { detail: 'Service unavailable' },
      };
      service.addToContext('apiId', apiId);

      // When
      service.promptEL(prompt).subscribe((response) => {
        if (typeof response['message'] !== 'string') {
          fail('Expected error but got error response');
        }
        done();
      });

      // Handle HTTP request
      const req = httpTestingController.expectOne(`${mockConstants.env.v2BaseURL}/newtai/el/_generate`);

      req.flush(errorResponse, {
        status: 500,
        statusText: 'Internal Server Error',
      });
    });
  });
});
