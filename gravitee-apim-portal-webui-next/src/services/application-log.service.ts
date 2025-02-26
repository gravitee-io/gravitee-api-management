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
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { Log, LogsResponse } from '../entities/log/log';

export interface ResponseTimeRange {
  to?: number;
  from?: number;
}

export interface SearchApplicationLogsParameters {
  to?: number;
  from?: number;
  apiIds?: string[];
  methods?: string[];
  responseTimeRanges?: ResponseTimeRange[];
  requestId?: string;
  transactionId?: string;
  statuses?: string[];
  messageText?: string;
  path?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApplicationLogService {
  public static METHODS: string[] = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD', 'TRACE', 'CONNECT', 'OTHER'];
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  search(
    applicationId: string,
    page: number = 1,
    pageSize: number = 10,
    params: SearchApplicationLogsParameters = {},
  ): Observable<LogsResponse> {
    const paramsList = [];
    paramsList.push(`page=${page}`);
    paramsList.push(`size=${pageSize}`);
    const paginationParams = `page=${page}&size=${pageSize}`;

    const currentDate = Date.now();
    const yesterdayDate = new Date();
    yesterdayDate.setDate(yesterdayDate.getDate() - 1);

    return this.http.post<LogsResponse>(`${this.configService.baseURL}/applications/${applicationId}/logs/_search?${paginationParams}`, {
      from: params.from ?? yesterdayDate.getTime(),
      to: params.to ?? currentDate,
      ...(params.apiIds?.length ? { apiIds: params.apiIds } : {}),
      ...(params.methods?.length ? { methods: params.methods } : {}),
      ...(params.requestId ? { requestIds: [params.requestId] } : {}),
      ...(params.transactionId ? { transactionIds: [params.transactionId] } : {}),
      ...(params.statuses?.length ? { statuses: params.statuses } : {}),
      ...(params.messageText ? { messageText: params.messageText } : {}),
      ...(params.path ? { path: params.path } : {}),
      ...(params.responseTimeRanges?.length ? { responseTimeRanges: params.responseTimeRanges } : {}),
    });
  }

  get(applicationId: string, logId: string, timestamp: string): Observable<Log> {
    return this.http.get<Log>(`${this.configService.baseURL}/applications/${applicationId}/logs/${logId}?timestamp=${timestamp}`);
  }
}
