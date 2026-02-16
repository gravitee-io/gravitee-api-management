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
import { ElAiPromptState, FeedbackSubmission } from '@gravitee/ui-particles-angular';
import { provideHttpClient } from '@angular/common/http';

import { NewtAIService } from './newtai.service';
import { SnackBarService } from './snack-bar.service';

import { Constants } from '../entities/Constants';

describe('NewtAIService', () => {
  let service: NewtAIService;
  let httpTestingController: HttpTestingController;
  const mockConstants = {
    env: {
      v2BaseURL: 'https://api.gravitee.io/management/v2',
    },
  };

  const mockSnackBarService = {
    error: jest.fn(),
    success: jest.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        NewtAIService,
        { provide: Constants, useValue: mockConstants },
        { provide: SnackBarService, useValue: mockSnackBarService },
      ],
    });

    service = TestBed.inject(NewtAIService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  describe('promptEL', () => {
    it('should make a POST request and return mapped ElAiPromptState', done => {
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
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      };
      service.addToContext('apiId', apiId);

      // When
      service.promptEL(prompt).subscribe(result => {
        // Then
        expect(result).toEqual(expectedResult);
        done();
      });

      const req = httpTestingController.expectOne(`${mockConstants.env.v2BaseURL}/newtai/el/_generate`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({ message: prompt, context: { apiId } });

      req.flush(mockResponse);
    });

    it('should propagate error when the API call fails', done => {
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
      service.promptEL(prompt).subscribe(response => {
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

  describe('submitFeedback', () => {
    it('should make a POST request when feedback is helpful', done => {
      // Given
      const feedbackSubmission: FeedbackSubmission = {
        feedback: 'helpful',
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      };

      // When
      service.submitFeedback(feedbackSubmission).subscribe(() => {
        // Then
        done();
      });

      const req = httpTestingController.expectOne(`${mockConstants.env.v2BaseURL}/newtai/el/feedback`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        answerHelpful: true,
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      });

      req.flush(null, { status: 200, statusText: 'OK' });
    });

    it('should make a POST request with answerHelpful=false when feedback is not helpful', done => {
      // Given
      const feedbackSubmission: FeedbackSubmission = {
        feedback: 'not-helpful',
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      };

      // When
      service.submitFeedback(feedbackSubmission).subscribe(() => {
        // Then
        done();
      });

      const req = httpTestingController.expectOne(`${mockConstants.env.v2BaseURL}/newtai/el/feedback`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual({
        answerHelpful: false,
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      });

      req.flush(null, { status: 200, statusText: 'OK' });
    });

    it('should handle error gracefully and show error snackbar when API call fails', done => {
      // Given
      const feedbackSubmission: FeedbackSubmission = {
        feedback: 'helpful',
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      };
      const errorResponse = {
        message: 'Failed to submit feedback',
      };

      // When
      service.submitFeedback(feedbackSubmission).subscribe({
        next: () => fail('should not emit next when error occurs'),
        error: () => fail('should not propagate error to subscriber'),
        complete: () => {
          expect(mockSnackBarService.error).toHaveBeenCalledWith('Failed to submit feedback');
          done();
        },
      });

      const req = httpTestingController.expectOne(`${mockConstants.env.v2BaseURL}/newtai/el/feedback`);
      expect(req.request.method).toEqual('POST');
      req.flush(errorResponse, {
        status: 500,
        statusText: 'Internal Server Error',
      });
    });

    it('should show success snackbar when feedback is submitted successfully', done => {
      // Given
      const feedbackSubmission: FeedbackSubmission = {
        feedback: 'helpful',
        feedbackRequestId: {
          chatId: 'chat-123',
          userMessageId: 'user-msg-123',
          agentMessageId: 'agent-msg-123',
        },
      };

      // When
      service.submitFeedback(feedbackSubmission).subscribe(() => {
        expect(mockSnackBarService.success).toHaveBeenCalledWith('Thanks for your feedback!');
        done();
      });

      const req = httpTestingController.expectOne(`${mockConstants.env.v2BaseURL}/newtai/el/feedback`);
      expect(req.request.method).toEqual('POST');
      req.flush(null, { status: 200, statusText: 'OK' });
    });
  });
});
