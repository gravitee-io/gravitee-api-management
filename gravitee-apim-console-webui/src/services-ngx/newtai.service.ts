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
import { Observable, of } from 'rxjs';
import { ElAiPromptState, GioElService } from '@gravitee/ui-particles-angular';
import { catchError, map } from 'rxjs/operators';

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
  private context: Partial<Record<NewtAIContextKeys, string>> = {};

  constructor() {
    this.gioElService.promptCallback = (p) => this.promptEL(p);
  }

  public promptEL(prompt: string): Observable<ElAiPromptState> {
    return this.httpClient
      .post<NewtAIResponse>(`${this.constants.env.v2BaseURL}/newtai/el/_generate`, { message: prompt, context: this.context })
      .pipe(
        map(({ message }) => ({ el: message })),
        catchError((error) => of({ message: error['message'] as string })),
      );
  }

  public addToContext(key: NewtAIContextKeys, value: string): void {
    this.context[key] = value;
  }

  public removeToContext(key: NewtAIContextKeys): void {
    delete this.context[key];
  }
}
