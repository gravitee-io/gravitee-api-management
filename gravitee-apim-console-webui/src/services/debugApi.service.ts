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
import { IHttpService, IPromise } from 'angular';

import { Constants } from '../entities/Constants';
import { Event } from '../entities/event/event';
import { IfMatchEtagInterceptor } from '../shared/interceptors/if-match-etag.interceptor';

export class DebugApiService {
  constructor(
    private readonly $http: IHttpService,
    private readonly Constants: Constants,
    private readonly ngIfMatchEtagInterceptor: IfMatchEtagInterceptor,
  ) {}

  public debug(
    api: any,
    request: {
      path: string;
      method: string;
      headers: Record<string, string[]>;
      body: string;
    },
  ): IPromise<Event> {
    // clean endpoint http proxy
    if (api.proxy && api.proxy.endpoints) {
      api.proxy.endpoints.forEach((endpoint) => {
        if (endpoint.proxy && !endpoint.proxy.useSystemProxy && (!endpoint.proxy.host || !endpoint.proxy.port)) {
          delete endpoint.proxy;
        }
      });
    }

    return this.$http
      .post<Event>(`${this.Constants.env.baseURL}/apis/${api.id}/_debug`, {
        version: api.version,
        description: api.description,
        proxy: api.proxy,
        paths: api.paths,
        flows: api.flows,
        plans: api.plans,
        private: api.private,
        visibility: api.visibility,
        name: api.name,
        services: api.services,
        properties: api.properties,
        tags: api.tags,
        picture: api.picture,
        picture_url: api.picture_url,
        background: api.background,
        background_url: api.background_url,
        resources: api.resources,
        categories: api.categories,
        groups: api.groups,
        labels: api.labels,
        path_mappings: api.path_mappings,
        response_templates: api.response_templates,
        lifecycle_state: api.lifecycle_state,
        disable_membership_notifications: api.disable_membership_notifications,
        flow_mode: api.flow_mode,
        gravitee: api.gravitee,
        request,
      })
      .then((value) => value.data);
  }
}
DebugApiService.$inject = ['$http', 'Constants', 'ngIfMatchEtagInterceptor'];
