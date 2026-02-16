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
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EMPTY, Observable, of } from 'rxjs';
import { ElAiPromptState, FeedbackSubmission, GioElService } from '@gravitee/ui-particles-angular';
import { catchError, map, tap } from 'rxjs/operators';

import { SnackBarService } from './snack-bar.service';

import { Constants } from '../entities/Constants';

interface NewtAIResponse {
  message: string;
  feedbackRequestId: {
    chatId: string;
    userMessageId: string;
    agentMessageId: string;
  };
}

export type NewtAIContextKeys = 'apiId';

@Injectable({
  providedIn: 'root',
})
export class NewtAIService {
  private httpClient = inject(HttpClient);
  private constants = inject(Constants);
  private gioElService = inject(GioElService);
  private snackBarService = inject(SnackBarService);
  private context: Partial<Record<NewtAIContextKeys, string>> = {};

  constructor() {
    if (this.constants?.org?.settings?.elGen?.enabled ?? false) {
      this.gioElService.promptCallback = p => this.promptEL(p);
      this.gioElService.feedbackCallback = fs => this.submitFeedback(fs);
    }
  }

  public promptEL(prompt: string): Observable<ElAiPromptState> {
    return this.httpClient
      .post<NewtAIResponse>(`${this.constants.env.v2BaseURL}/newtai/el/_generate`, { message: prompt, context: this.context })
      .pipe(
        map(({ message, feedbackRequestId }) => ({
          el: message,
          feedbackRequestId,
        })),
        catchError(error => of({ message: error['message'] as string })),
      );
  }
  public submitFeedback(fs: FeedbackSubmission): Observable<void> {
    return this.httpClient
      .post<void>(`${this.constants.env.v2BaseURL}/newtai/el/feedback`, {
        answerHelpful: fs.feedback === 'helpful',
        feedbackRequestId: {
          chatId: fs.feedbackRequestId?.chatId,
          userMessageId: fs.feedbackRequestId?.userMessageId,
          agentMessageId: fs.feedbackRequestId?.agentMessageId,
        },
      })
      .pipe(
        tap(() => this.snackBarService.success('Thanks for your feedback!')),
        catchError(error => {
          this.snackBarService.error(error.error?.message ?? 'Error while submitting feedback.');
          return EMPTY;
        }),
      );
  }

  public addToContext(key: NewtAIContextKeys, value: string): void {
    this.context[key] = value;
  }

  public removeToContext(key: NewtAIContextKeys): void {
    delete this.context[key];
  }
}
