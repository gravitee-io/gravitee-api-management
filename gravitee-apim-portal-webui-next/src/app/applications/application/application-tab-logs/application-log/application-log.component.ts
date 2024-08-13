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
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIcon } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, EMPTY, map, Observable, switchMap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { CopyCodeComponent } from '../../../../../components/copy-code/copy-code.component';
import { LoaderComponent } from '../../../../../components/loader/loader.component';
import { Log, LogMetadataApi, LogMetadataPlan } from '../../../../../entities/log/log';
import { ApplicationLogService } from '../../../../../services/application-log.service';

interface LogVM extends Log {
  apiName: string;
  planName: string;
  requestHeaders: { key: string; value: string }[];
  responseHeaders: { key: string; value: string }[];
}

@Component({
  selector: 'app-application-log',
  standalone: true,
  imports: [AsyncPipe, LoaderComponent, MatExpansionModule, MatCard, MatCardContent, MatIcon, RouterLink, DatePipe, CopyCodeComponent],
  templateUrl: './application-log.component.html',
  styleUrl: './application-log.component.scss',
})
export class ApplicationLogComponent implements OnInit {
  @Input()
  applicationId!: string;

  @Input()
  logId!: string;

  log$: Observable<LogVM> = of();
  error: boolean = false;

  constructor(
    private applicationLogService: ApplicationLogService,
    private activatedRoute: ActivatedRoute,
  ) {}
  ngOnInit() {
    this.log$ = this.activatedRoute.queryParams.pipe(
      switchMap(params => {
        if (params['timestamp']) {
          return this.applicationLogService.get(this.applicationId, this.logId, params['timestamp']);
        }
        this.error = true;
        return EMPTY;
      }),
      map(log => {
        const apiName = log.metadata?.[log.api]
          ? `${(log.metadata[log.api] as LogMetadataApi).name} (${(log.metadata[log.api] as LogMetadataApi).version})`
          : '';
        const planName = log.metadata?.[log.plan] ? (log.metadata[log.plan] as LogMetadataPlan).name : '';
        const requestHeaders = log.request?.headers
          ? Object.entries(log.request.headers).map(keyValueArray => ({ key: keyValueArray[0], value: keyValueArray[1] }))
          : [];
        const responseHeaders = log.response?.headers
          ? Object.entries(log.response.headers).map(keyValueArray => ({ key: keyValueArray[0], value: keyValueArray[1] }))
          : [];
        return { ...log, apiName, planName, requestHeaders, responseHeaders };
      }),
      catchError(err => {
        console.error(err);
        this.error = true;
        return EMPTY;
      }),
    );
  }
}
