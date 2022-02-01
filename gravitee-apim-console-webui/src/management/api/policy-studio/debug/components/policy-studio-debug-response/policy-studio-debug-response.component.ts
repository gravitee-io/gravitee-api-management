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

import { Component, Input, OnChanges } from '@angular/core';

import { DebugResponse } from '../../models/DebugResponse';
import { HttpStatusCodeDescription } from '../../models/HttpStatusCodeDescription';

type ResponseDisplayableVM = {
  statusCode: number;
  statusCodeDescription: string;
  successfulRequest: boolean;
  errorRequest: boolean;
  gvCodeOptions: Record<string, any>;
  prettifiedResponse: string;
  headers: { name: string; value: string }[];
};

@Component({
  selector: 'policy-studio-debug-response',
  template: require('./policy-studio-debug-response.component.html'),
  styles: [require('./policy-studio-debug-response.component.scss')],
})
export class PolicyStudioDebugResponseComponent implements OnChanges {
  @Input()
  public debugResponse?: DebugResponse;

  public responseDisplayableVM: ResponseDisplayableVM;

  ngOnChanges(): void {
    if (this.debugResponse && !this.debugResponse.isLoading) {
      const contentTypeHeader = Object.entries(this.debugResponse.response.headers).find(([key]) => {
        return key.toLowerCase() === 'content-type';
      });
      const contentType = contentTypeHeader ? contentTypeHeader[1].split(';')[0] : undefined;

      const gvCodeOptions = {
        lineNumbers: true,
        mode: 'text',
      };
      let prettifiedResponse = this.debugResponse.response.body;

      if (contentType === 'text/html') {
        gvCodeOptions.mode = 'htmlmixed';
      } else if (contentType === 'application/json') {
        gvCodeOptions.mode = 'application/json';
        // Convert JSON body to a human readable form with 2 spaces indentation
        prettifiedResponse = JSON.stringify(JSON.parse(this.debugResponse.response.body), null, 2);
      }

      this.responseDisplayableVM = {
        statusCode: this.debugResponse.response.statusCode,
        statusCodeDescription: HttpStatusCodeDescription[this.debugResponse.response.statusCode].description,
        successfulRequest: 200 <= this.debugResponse?.response.statusCode && this.debugResponse?.response.statusCode < 300,
        errorRequest: 400 <= this.debugResponse?.response.statusCode && this.debugResponse?.response.statusCode < 600,
        gvCodeOptions,
        prettifiedResponse,
        headers: Object.entries(this.debugResponse.response.headers).map(([key, value]) => ({ name: key, value: value })),
      };
    }
  }
}
