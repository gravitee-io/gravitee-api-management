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
import { Group } from 'src/entities/group/group';

import { Component, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { FormControl, FormGroup } from '@angular/forms';

import { GroupService } from '../../../../../services-ngx/group.service';
import { ApplicationService } from '../../../../../services-ngx/application.service';
import { Application } from '../../../../../entities/application/Application';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'application-general-access-groups',
  templateUrl: './application-general-groups.component.html',
  styleUrls: ['./application-general-groups.component.scss'],
})
export class ApplicationGeneralGroupsComponent implements OnInit, OnDestroy {
  public groups: Group[];
  public application: Application;
  public form: FormGroup;
  public initialFormValue: unknown;
  public isReadonly = false;

  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly groupService: GroupService,
    private readonly applicationService: ApplicationService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    combineLatest([this.applicationService.getById(this.activatedRoute.snapshot.params.applicationId), this.groupService.list()])
      .pipe(
        tap(([application, groups]) => {
          this.groups = groups;
          this.application = application;
          this.isReadonly = application.origin === 'KUBERNETES';
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        const userGroupList: Group[] = this.groups.filter((group) => this.application.groups?.includes(group.id));
        this.form = new FormGroup({
          selectedGroups: new FormControl(userGroupList.map((group) => group.id)),
        });

        if (this.isReadonly) {
          this.form.disable({ emitEvent: false });
        }

        this.initialFormValue = this.form.getRawValue();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save() {
    this.applicationService
      .update({
        ...this.application,
        groups: this.form.getRawValue()?.selectedGroups ?? this.initialFormValue,
      })
      .pipe(
        tap(() => {
          this.snackBarService.success('Changes successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
