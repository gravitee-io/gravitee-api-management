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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { sortBy } from 'lodash';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../services-ngx/api.service';
import { Api } from '../../../../entities/api';

interface PathMappingDS {
  path: string;
}

@Component({
  selector: 'api-path-mappings',
  template: require('./api-path-mappings.component.html'),
  styles: [require('./api-path-mappings.component.scss')],
})
export class ApiPathMappingsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public displayedColumns = ['path', 'actions'];
  public pathMappingsDS: PathMappingDS[] = [];
  public isLoadingData = true;

  constructor(@Inject(UIRouterStateParams) private readonly ajsStateParams, private readonly apiService: ApiService) {}

  public ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.pathMappingsDS = this.toPathMappingDS(api);
          this.isLoadingData = false;
        }),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private toPathMappingDS(api: Api): PathMappingDS[] {
    return sortBy(api.path_mappings).map((path) => ({ path }));
  }
}
