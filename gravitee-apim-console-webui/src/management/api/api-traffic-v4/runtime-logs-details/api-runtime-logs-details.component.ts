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
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';
import { onlyApiV4Filter } from '../../../../util/apiFilter.operator';

@Component({
  selector: 'api-runtime-logs-details',
  templateUrl: './api-runtime-logs-details.component.html',
  standalone: false,
})
export class ApiRuntimeLogsDetailsComponent {
  api$: Observable<ApiV4> = this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV4Filter());
  apiType = toSignal(this.api$.pipe(map(api => api.type)));

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
  ) {}
}
