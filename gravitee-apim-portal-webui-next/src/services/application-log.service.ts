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

export interface HttpMethodVM {
  value: string;
  label: string;
}

export interface ApplicationLogsListParameters {
  page?: number;
  size?: number;
  to?: number;
  from?: number;
  order?: 'ASC' | 'DESC';
  field?: string;
  apis?: string[];
  methods?: HttpMethodVM[];
  responseTimes?: string[];
  requestId?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApplicationLogService {
  public static METHODS: HttpMethodVM[] = [
    { value: '3', label: 'GET' },
    { value: '7', label: 'POST' },
    { value: '8', label: 'PUT' },
    { value: '2', label: 'DELETE' },
    { value: '6', label: 'PATCH' },
    { value: '5', label: 'OPTIONS' },
    { value: '4', label: 'HEAD' },
    { value: '9', label: 'TRACE' },
    { value: '1', label: 'CONNECT' },
    { value: '0', label: 'OTHER' },
  ];
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  list(applicationId: string, params: ApplicationLogsListParameters): Observable<LogsResponse> {
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

  private serializeLogListQuery(params: ApplicationLogsListParameters): string {
    const queryList: string[] = [];
    const apisQuerySegment = this.createQuotationsListQuerySegment('api', params.apis);
    if (apisQuerySegment) {
      queryList.push(apisQuerySegment);
    }

    const methodsQuerySegment = this.createQuotationsListQuerySegment(
      'method',
      params.methods?.map(m => m.value),
    );
    if (methodsQuerySegment) {
      queryList.push(methodsQuerySegment);
    }

    const responseTimesQuerySegment = this.createListQuerySegment('response-time', params.responseTimes);
    if (responseTimesQuerySegment) {
      queryList.push(responseTimesQuerySegment);
    }

    const requestIdQuerySegment = this.createQuotationsQuerySegment('_id', params.requestId);
    if (requestIdQuerySegment) {
      queryList.push(requestIdQuerySegment);
    }

    return queryList.join(' AND ');
  }

  private createQuotationsQuerySegment(root: string, value?: string) {
    if (value && value.length) {
      return this.createQuotationsListQuerySegment(root, [value]);
    }
    return undefined;
  }

  private createQuotationsListQuerySegment(root: string, values?: string[]) {
    if (values?.length) {
      const queryContent = values.map(v => `\\"${v}\\"`).join(' OR ');
      return `(${root}\:${queryContent})`;
    }
    return undefined;
  }

  private createListQuerySegment(root: string, values?: string[]) {
    if (values?.length) {
      const queryContent = values.map(v => `[${v}]`).join(' OR ');
      return `(${root}\:${queryContent})`;
    }
    return undefined;
  }
}
