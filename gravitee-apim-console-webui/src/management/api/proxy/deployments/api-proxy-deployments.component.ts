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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { Tag } from '../../../../entities/tag/tag';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { TagService } from '../../../../services-ngx/tag.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { onlyApiV2Filter } from '../../../../util/apiFilter.operator';

@Component({
  selector: 'api-proxy-deployments',
  templateUrl: './api-proxy-deployments.component.html',
  styleUrls: ['./api-proxy-deployments.component.scss'],
})
export class ApiProxyDeploymentsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public shardingTags: Tag[];
  public deploymentsForm: UntypedFormGroup;
  public initialDeploymentsFormValue: unknown;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly tagService: TagService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    combineLatest([this.apiService.get(this.activatedRoute.snapshot.params.apiId), this.tagService.list()])
      .pipe(
        tap(([api, shardingTags]) => {
          this.shardingTags = shardingTags;

          const isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u'])  || api.definitionContext?.origin === 'KUBERNETES' || api.definitionContext?.origin === 'AWS' || api.definitionContext?.origin === 'SOLACE';

          this.deploymentsForm = new UntypedFormGroup({
            tags: new UntypedFormControl({
              value: api.tags ?? [],
              disabled: isReadOnly,
            }),
          });

          this.initialDeploymentsFormValue = this.deploymentsForm.getRawValue();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    return this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => this.apiService.update(api.id, { ...api, tags: this.deploymentsForm.get('tags').value ?? [] })),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
