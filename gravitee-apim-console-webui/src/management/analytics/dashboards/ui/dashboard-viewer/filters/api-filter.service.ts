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
import { ResultsLoaderInput, ResultsLoaderOutput, SelectOption } from '@gravitee/gravitee-dashboard';

import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  HttpListener,
  KafkaListener,
  Listener,
  SubscriptionListener,
  TcpListener,
  Api,
} from '../../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';

@Injectable({
  providedIn: 'root',
})
export class ApiFilterService {
  private readonly itemsPerPage = 10;
  private readonly apiV2Service = inject(ApiV2Service);

  resultsLoader = (input: ResultsLoaderInput): Observable<ResultsLoaderOutput> => {
    return this.apiV2Service.search({ query: input.searchTerm }, undefined, input.page, this.itemsPerPage).pipe(
      map((response) => ({
        data: response.data.map((api) => ({ value: api.id, label: api.name, context: this.getEntrypoint(api) }) satisfies SelectOption),
        hasNextPage: response.pagination.pageCount > input.page,
      })),
    );
  };

  private isHttpListener(listener: Listener): listener is HttpListener {
    return listener.type === 'HTTP';
  }
  private isKafkaListener(listener: Listener): listener is KafkaListener {
    return listener.type === 'KAFKA';
  }
  private isTcpListener(listener: Listener): listener is TcpListener {
    return listener.type === 'TCP';
  }
  private isSubscriptionListener(listener: Listener): listener is SubscriptionListener {
    return listener.type === 'SUBSCRIPTION';
  }

  private getEntrypoint(api: Api): string | undefined {
    if (api.definitionVersion === 'V2') {
      return api.contextPath;
    }

    if (api.definitionVersion === 'V4') {
      const listener = api.listeners.find((listener) => !this.isSubscriptionListener(listener).valueOf());
      if (!listener) return undefined;

      if (this.isHttpListener(listener)) {
        return listener?.paths[0]?.path;
      }
      if (this.isKafkaListener(listener)) {
        return listener?.host;
      }
      if (this.isTcpListener(listener)) {
        return listener?.hosts[0];
      }
    }
    return undefined;
  }
}
