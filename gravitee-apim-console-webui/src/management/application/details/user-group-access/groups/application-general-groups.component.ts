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
import { GroupV2Service } from 'src/services-ngx/group-v2.service';

import { Component, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { FormControl, FormGroup } from '@angular/forms';

import { ApplicationService } from '../../../../../services-ngx/application.service';
import { Application } from '../../../../../entities/application/Application';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'application-general-access-groups',
  templateUrl: './application-general-groups.component.html',
  styleUrls: ['./application-general-groups.component.scss'],
})
export class ApplicationGeneralGroupsComponent implements OnInit, OnDestroy {
  public groups: Group[] = [];
  public application: Application;
  public form: FormGroup;
  public initialFormValue: unknown;
  public isReadonly = false;

  public page = 1;
  public pageSize = 50;
  public hasMoreGroups = true;
  public isLoading = false;

  private unsubscribe$: Subject<void> = new Subject<void>();
  private scrollContainer: HTMLElement | null = null;
  private scrollListener: (() => void) | null = null;
  private totalPages = 0;

  constructor(
    private readonly groupv2Service: GroupV2Service,
    private readonly applicationService: ApplicationService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.applicationService
      .getById(this.activatedRoute.snapshot.params.applicationId)
      .pipe(
        tap((application) => {
          this.application = application;
          this.isReadonly = application.origin === 'KUBERNETES';
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.loadInitialGroups();
      });
  }

  /**
   * Load first page + selected groups together
   */
  private loadInitialGroups(): void {
    this.isLoading = true;
    const selectedGroupIds = this.application.groups ?? [];

    combineLatest([
      this.groupv2Service.list(1, this.pageSize),
      selectedGroupIds.length > 0 ? this.groupv2Service.listById(selectedGroupIds, 1, selectedGroupIds.length) : EMPTY,
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([page1Res, selectedRes]: any) => {
        const page1Groups: Group[] = page1Res?.data ?? [];
        const selectedGroups: Group[] = Array.isArray(selectedRes) ? selectedRes : selectedRes?.data ?? [];

        // Deduplicate and order: selected groups first
        const selectedMap = new Map(selectedGroups.map((g) => [g.id, g]));
        const otherGroups = page1Groups.filter((g) => !selectedMap.has(g.id));

        this.groups = [...selectedGroups, ...otherGroups];

        const pagination = page1Res?.pagination;
        if (pagination) {
          this.totalPages = pagination.pageCount;
          this.page = pagination.page + 1;
          this.hasMoreGroups = this.page <= this.totalPages;
        }

        const userGroupList: Group[] = this.groups.filter((g) => this.application.groups?.includes(g.id));
        this.form = new FormGroup({
          selectedGroups: new FormControl(userGroupList.map((g) => g.id)),
        });

        if (this.isReadonly) {
          this.form.disable({ emitEvent: false });
        }

        this.initialFormValue = this.form.getRawValue();

        this.isLoading = false;
      });
  }

  private loadGroups(page: number): void {
    if (this.isLoading || !this.hasMoreGroups) return;

    this.isLoading = true;
    this.groupv2Service
      .list(page, this.pageSize)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((res: any) => {
        const pagination = res?.pagination;
        const newGroups: Group[] = res?.data ?? [];

        // Deduplicate, keep selected groups at the top
        const selectedIds = new Set(this.application.groups ?? []);
        const existingIds = new Set(this.groups.map((g) => g.id));

        const filteredNewGroups = newGroups.filter((g) => !existingIds.has(g.id));

        // Append only non-selected groups (to keep order)
        this.groups = [
          ...this.groups.filter((g) => selectedIds.has(g.id)),
          ...this.groups.filter((g) => !selectedIds.has(g.id)),
          ...filteredNewGroups,
        ];

        if (pagination) {
          this.totalPages = pagination.pageCount;
          if (pagination.page >= this.totalPages) {
            this.hasMoreGroups = false;
          } else {
            this.page = pagination.page + 1;
          }
        }

        this.isLoading = false;
      });
  }

  onSelectToggle(opened: boolean): void {
    if (opened) {
      const checkInterval = setInterval(() => {
        this.scrollContainer = document.querySelector('div[role="listbox"][aria-multiselectable="true"]') as HTMLElement;
        if (this.scrollContainer) {
          clearInterval(checkInterval);
          const boundScrollHandler = this.onScroll.bind(this);
          this.scrollContainer.addEventListener('scroll', boundScrollHandler);
          this.scrollListener = () => this.scrollContainer?.removeEventListener('scroll', boundScrollHandler);
        }
      }, 50);
    } else {
      this.cleanupScrollListener();
    }
  }

  private onScroll(event: Event): void {
    const target = event.target as HTMLElement;
    const threshold = 0.7 * target.scrollHeight;

    if (target.scrollTop + target.clientHeight >= threshold && this.hasMoreGroups && !this.isLoading) {
      this.loadGroups(this.page);
    }
  }

  private cleanupScrollListener(): void {
    if (this.scrollListener) {
      this.scrollListener();
    }
    this.scrollListener = null;
    this.scrollContainer = null;
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
    this.cleanupScrollListener();
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
