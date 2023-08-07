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
import { FormBuilder, FormGroup } from '@angular/forms';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject, combineLatest } from 'rxjs';

import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { GroupService } from '../../../../../services-ngx/group.service';
import { Group } from '../../../../../entities/group/group';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';

@Component({
  selector: 'api-general-access-groups',
  template: require('./api-general-groups.component.html'),
  styles: [require('./api-general-groups.component.scss')],
})
export class ApiGeneralGroupsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public isReadOnly = true;
  public form: FormGroup;
  public groups: Group[];
  public initialFormValue: unknown;
  public readOnlyGroupList: string;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiV2Service,
    private readonly groupService: GroupService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly formBuilder: FormBuilder,
  ) {}

  ngOnInit() {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']);
    combineLatest([this.groupService.list(), this.apiService.get(this.ajsStateParams.apiId)])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([groups, api]) => {
        this.groups = groups;

        const userGroupList: Group[] = this.groups.filter((group) => api.groups?.includes(group.id));
        this.form = this.formBuilder.group({
          selectedGroups: {
            value: userGroupList.map((g) => g.id),
            disabled: this.isReadOnly,
          },
        });
        this.initialFormValue = this.form.getRawValue();
        this.readOnlyGroupList = userGroupList.length === 0 ? 'No groups associated' : userGroupList.map((g) => g.name).join(', ');
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save() {
    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api) => {
          if (api.definitionVersion !== 'V1') {
            return this.apiService.update(api.id, {
              ...api,
              groups: this.form.getRawValue()?.selectedGroups ?? this.initialFormValue,
            });
          }
          // V1 API edition not supported
          return EMPTY;
        }),
        tap(
          () => this.snackBarService.success('Configuration successfully saved!'),
          ({ error }) => {
            this.snackBarService.error(error.message);
            return EMPTY;
          },
        ),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
