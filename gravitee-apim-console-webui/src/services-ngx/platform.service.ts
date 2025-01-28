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

import { Inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { PlatformLogsQueryParams, PlatformLogsResponse } from '../entities/platform/platformLogs';
import { Constants } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class PlatformService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getPlatformV2Logs(params: PlatformLogsQueryParams): Observable<PlatformLogsResponse> {
    // TODO in APIM-7863: replace this mocked reply with backend when ready.
    return of({
      logs: [],
      total: params.size,
    });
  }
}
