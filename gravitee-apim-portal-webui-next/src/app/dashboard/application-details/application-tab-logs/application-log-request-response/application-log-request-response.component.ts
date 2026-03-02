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
import { Component, computed, input, Signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';

import { CopyCodeComponent } from '../../../../../components/copy-code/copy-code.component';
import { Log, LogMetadataApi, LogMetadataPlan } from '../../../../../entities/log';

interface LogVM extends Log {
  apiName: string;
  planName: string;
  requestHeaders: { key: string; value: string }[];
  responseHeaders: { key: string; value: string }[];
}

@Component({
  selector: 'app-application-log-request-response',
  standalone: true,
  imports: [CopyCodeComponent, MatCardModule, MatExpansionModule],
  templateUrl: './application-log-request-response.component.html',
  styleUrl: './application-log-request-response.component.scss',
})
export class ApplicationLogRequestResponseComponent {
  log = input.required<Log>();
  logVM: Signal<LogVM> = computed(() => {
    const log = this.log();

    const apiName = log.metadata?.[log.api]
      ? `${(log.metadata[log.api] as LogMetadataApi).name} (${(log.metadata[log.api] as LogMetadataApi).version})`
      : '';
    const apiType = (log.metadata?.[log.api] as LogMetadataApi)?.apiType;
    const planName = log.metadata?.[log.plan] ? (log.metadata[log.plan] as LogMetadataPlan).name : '';
    const requestHeaders = log.request?.headers
      ? Object.entries(log.request.headers).map(keyValueArray => ({ key: keyValueArray[0], value: keyValueArray[1] }))
      : [];
    const responseHeaders = log.response?.headers
      ? Object.entries(log.response.headers).map(keyValueArray => ({ key: keyValueArray[0], value: keyValueArray[1] }))
      : [];
    return { ...log, apiName, apiType, planName, requestHeaders, responseHeaders };
  });
}
