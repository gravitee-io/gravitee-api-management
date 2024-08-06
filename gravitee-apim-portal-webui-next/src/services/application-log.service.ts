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
import { LogsResponse } from '../entities/log/log';

/* eslint-disable no-useless-escape */

@Injectable({
  providedIn: 'root',
})
export class ApplicationLogService {
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  list(
    applicationId: string,
    params: {
      page?: number;
      size?: number;
      to?: number;
      from?: number;
      order?: 'ASC' | 'DESC';
      field?: string;
      apis?: string[];
    },
  ): Observable<LogsResponse> {
    const paramsList = [];
    paramsList.push(`page=${params.page ?? 1}`);
    paramsList.push(`size=${params.size ?? 10}`);

    const currentDate = new Date();
    const yesterdayDate = new Date();
    yesterdayDate.setDate(yesterdayDate.getDate() - 1);

    paramsList.push(`from=${params.from ?? yesterdayDate.getTime()}`);
    paramsList.push(`to=${params.to ?? currentDate.getTime()}`);
    paramsList.push(`order=${params.order ?? 'DESC'}`);

    paramsList.push(`field=${params.field ?? '@timestamp'}`);

    const query = this.serializeLogListQuery({ ...params });
    if (query.length) {
      paramsList.push(`query=${query}`);
    }

    return this.http.get<LogsResponse>(`${this.configService.baseURL}/applications/${applicationId}/logs?${paramsList.join('&')}`);
  }

  private serializeLogListQuery(params: { apis?: string[] }): string {
    const queryList: string[] = [];
    if (params.apis?.length) {
      const apis = `(api\:${params.apis.map(api => `\\"${api}\\"`).join(' OR ')})`;
      queryList.push(apis);
    }
    return queryList.join(' AND ');
  }
}
