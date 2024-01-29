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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { switchMap, takeUntil } from 'rxjs/operators';
import { Subject, combineLatest, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { Group } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';

@Component({
  selector: 'api-general-access-groups',
  templateUrl: './api-general-groups.component.html',
  styleUrls: ['./api-general-groups.component.scss'],
})
export class ApiGeneralGroupsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public isReadOnly = true;
  public form: UntypedFormGroup;
  public groups: Group[];
  public initialFormValue: unknown;
  public readOnlyGroupList: string;
  private isV1Api = false;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly groupService: GroupV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly formBuilder: UntypedFormBuilder,
  ) {}

  ngOnInit() {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']);
    combineLatest([this.groupService.list(1, 9999), this.apiService.get(this.activatedRoute.snapshot.params.apiId)])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([groups, api]) => {
        this.isV1Api = api.definitionVersion === 'V1';
        this.groups = groups.data;

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
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api) =>
          api.definitionVersion === 'V1' || api.definitionVersion === 'FEDERATED'
            ? throwError({ message: 'You cannot modify a V1 API.' })
            : this.apiService.update(api.id, { ...api, groups: this.form.getRawValue()?.selectedGroups ?? this.initialFormValue }),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (_) => {
          this.form.markAsPristine(); // Close save bar
          this.snackBarService.success('Configuration successfully saved!');
          this.ngOnInit();
        },
        ({ error }) => this.snackBarService.error(error.message),
      );
  }
}
