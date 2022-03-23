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
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { Page } from '../../../projects/portal-webclient-sdk/src/lib';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  private readonly currentPageSource: BehaviorSubject<Page>;

  constructor() {
    this.currentPageSource = new BehaviorSubject<Page>(null);
  }

  disposePage() {
    this.currentPageSource.next(null);
  }

  get(): BehaviorSubject<Page> {
    return this.currentPageSource;
  }

  set(page: Page) {
    this.currentPageSource.next(page);
  }

  getCurrentPage() {
    return this.currentPageSource.getValue();
  }
}
