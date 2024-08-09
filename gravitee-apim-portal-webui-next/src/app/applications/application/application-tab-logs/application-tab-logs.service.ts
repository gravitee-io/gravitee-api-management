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
import { Injectable } from '@angular/core';

import { ApplicationLogService, HttpMethodVM } from '../../../../services/application-log.service';

export interface ResponseTimeVM {
  value: string;
  min: number;
  max?: number;
}

export interface PeriodVM {
  milliseconds: number;
  period: number;
  unit: 'MINUTE' | 'HOUR' | 'DAY';
  value: string;
}

export interface HttpStatusVM {
  label: string;
  value: string;
}

@Injectable({
  providedIn: 'root',
})
export class ApplicationTabLogsService {
  get httpMethods(): HttpMethodVM[] {
    return ApplicationLogService.METHODS;
  }

  get responseTimes(): ResponseTimeVM[] {
    return [
      {
        value: '0 TO 100',
        min: 0,
        max: 100,
      },
      {
        value: '100 TO 200',
        min: 100,
        max: 200,
      },
      {
        value: '200 TO 300',
        min: 200,
        max: 300,
      },
      {
        value: '300 TO 400',
        min: 300,
        max: 400,
      },
      {
        value: '400 TO 500',
        min: 400,
        max: 500,
      },
      {
        value: '500 TO 1000',
        min: 500,
        max: 1000,
      },
      {
        value: '1000 TO 2000',
        min: 1000,
        max: 2000,
      },
      {
        value: '2000 TO 5000',
        min: 2000,
        max: 5000,
      },
      {
        value: '5000 TO *',
        min: 5000,
      },
    ];
  }

  get periods(): PeriodVM[] {
    return [
      {
        milliseconds: this.toMilliseconds(0, 0, 5),
        period: 5,
        unit: 'MINUTE',
        value: '5m',
      },
      {
        milliseconds: this.toMilliseconds(0, 0, 30),
        period: 30,
        unit: 'MINUTE',
        value: '30m',
      },
      {
        milliseconds: this.toMilliseconds(0, 1, 0),
        period: 1,
        unit: 'HOUR',
        value: '1h',
      },
      {
        milliseconds: this.toMilliseconds(0, 3, 0),
        period: 3,
        unit: 'HOUR',
        value: '3h',
      },
      {
        milliseconds: this.toMilliseconds(0, 6, 0),
        period: 6,
        unit: 'HOUR',
        value: '6h',
      },
      {
        milliseconds: this.toMilliseconds(0, 12, 0),
        period: 12,
        unit: 'HOUR',
        value: '12h',
      },
      {
        milliseconds: this.toMilliseconds(1, 0, 0),
        period: 1,
        unit: 'DAY',
        value: '1d',
      },
      {
        milliseconds: this.toMilliseconds(3, 0, 0),
        period: 3,
        unit: 'DAY',
        value: '3d',
      },
      {
        milliseconds: this.toMilliseconds(7, 0, 0),
        period: 7,
        unit: 'DAY',
        value: '7d',
      },
      {
        milliseconds: this.toMilliseconds(14, 0, 0),
        period: 14,
        unit: 'DAY',
        value: '14d',
      },
      {
        milliseconds: this.toMilliseconds(30, 0, 0),
        period: 30,
        unit: 'DAY',
        value: '30d',
      },
      {
        milliseconds: this.toMilliseconds(90, 0, 0),
        period: 90,
        unit: 'DAY',
        value: '90d',
      },
    ];
  }

  get httpStatuses(): HttpStatusVM[] {
    return [
      { value: '100', label: '100 - Continue' },
      { value: '101', label: '101 - Switching Protocols' },
      { value: '102', label: '102 - Processing' },
      { value: '200', label: '200 - OK' },
      { value: '201', label: '201 - Created' },
      { value: '202', label: '202 - Accepted' },
      { value: '203', label: '203 - Non Authoritative Information' },
      { value: '204', label: '204 - No Content' },
      { value: '205', label: '205 - Reset Content' },
      { value: '206', label: '206 - Partial Content' },
      { value: '207', label: '207 - Multi Status' },
      { value: '300', label: '300 - Multiple Choices' },
      { value: '301', label: '301 - Moved Permanently' },
      { value: '302', label: '302 - Found' },
      { value: '303', label: '303 - See Other' },
      { value: '304', label: '304 - Not Modified' },
      { value: '305', label: '305 - Use Proxy' },
      { value: '307', label: '307 - Temporary Redirect' },
      { value: '400', label: '400 - Bad Request' },
      { value: '401', label: '401 - Unauthorized' },
      { value: '402', label: '402 - Payment Required' },
      { value: '403', label: '403 - Forbidden' },
      { value: '404', label: '404 - Not Found' },
      { value: '405', label: '405 - Method Not Allowed' },
      { value: '406', label: '406 - Not Acceptable' },
      { value: '407', label: '407 - Proxy Authentication Required' },
      { value: '408', label: '408 - Request Timeout' },
      { value: '409', label: '409 - Conflict' },
      { value: '410', label: '410 - Gone' },
      { value: '411', label: '411 - Length Required' },
      { value: '412', label: '412 - Precondition Failed' },
      { value: '413', label: '413 - Request Entity Too Large' },
      { value: '414', label: '414 - Request Uri Too Long' },
      { value: '415', label: '415 - Unsupported Media Type' },
      { value: '416', label: '416 - Requested Range Not Satisfiable' },
      { value: '417', label: '417 - Expectation Failed' },
      { value: '422', label: '422 - Unprocessable Entity' },
      { value: '423', label: '423 - Locked' },
      { value: '424', label: '424 - Failed Dependency' },
      { value: '429', label: '429 - Too Many Requests' },
      { value: '500', label: '500 - Internal Server Error' },
      { value: '501', label: '501 - Not Implemented' },
      { value: '502', label: '502 - Bad Gateway' },
      { value: '503', label: '503 - Service Unavailable' },
      { value: '504', label: '504 - Gateway Timeout' },
      { value: '505', label: '505 - Http Version Not Supported' },
      { value: '507', label: '507 - Insufficient Storage' },
    ];
  }

  private toMilliseconds(days: number, hours: number, minutes: number): number {
    return (days * 24 * 60 * 60 + hours * 60 * 60 + minutes * 60) * 1000;
  }
}
