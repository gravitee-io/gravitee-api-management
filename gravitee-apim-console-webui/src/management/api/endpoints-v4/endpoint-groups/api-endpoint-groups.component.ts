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
import { Component, OnDestroy } from '@angular/core';
import { map, takeUntil } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-endpoint-groups',
  templateUrl: './api-endpoint-groups.component.html',
  standalone: false,
})
export class ApiEndpointGroupsComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public api$: Observable<ApiV4> = this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(
    map(api => api as ApiV4),
    takeUntil(this.unsubscribe$),
  );

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
  ) {}

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }
}
