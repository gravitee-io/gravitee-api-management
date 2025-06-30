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
import { ErrorHandler, Injectable } from '@angular/core';

const CHUNK_LOAD_ERROR_REGEX = /Loading chunk [\d]+ failed/;
const CHUNK_LOAD_RETRIES_SESSION_KEY = 'chunkLoadRetries';
const MAX_RETRIES = 1;

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  handleError(error: unknown): void {
    if (this.isChunkLoadError(error)) {
      this.handleChunkLoadError();
    } else {
      // Forward all other errors
      // eslint-disable-next-line angular/log
      console.error(error);
    }
  }

  private isChunkLoadError(error: unknown): boolean {
    let message = '';
    if (typeof error === 'string') {
      message = error;
    } else if (error instanceof Error) {
      message = error.message;
    }
    return CHUNK_LOAD_ERROR_REGEX.test(message);
  }

  private handleChunkLoadError(): void {
    const retries = sessionStorage.getItem(CHUNK_LOAD_RETRIES_SESSION_KEY) || '0';
    const retryCount = parseInt(retries, 10);

    if (retryCount < MAX_RETRIES) {
      sessionStorage.setItem(CHUNK_LOAD_RETRIES_SESSION_KEY, (retryCount + 1).toString());
      window.location.reload();
    } else {
      // eslint-disable-next-line angular/log
      console.error('Chunk loading failed multiple times. Please refresh manually.');
    }
  }
}
