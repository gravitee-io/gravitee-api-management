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

import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { catchError, map, takeUntil, tap, switchMap, take } from 'rxjs/operators';
import { FormControl, FormGroup } from '@angular/forms';
import { MatSelect } from '@angular/material/select';

import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { ApplicationService } from '../../../../../services-ngx/application.service';
import { Application } from '../../../../../entities/application/Application';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

interface Pagination {
  page: number;
  pageCount: number;
  perPage?: number;
  totalCount?: number;
}

@Component({
  selector: 'application-general-access-groups',
  templateUrl: './application-general-groups.component.html',
  styleUrls: ['./application-general-groups.component.scss'],
  standalone: false,
})
export class ApplicationGeneralGroupsComponent implements OnInit, OnDestroy {
  @ViewChild('groupsMatSelect') groupMatSelect!: MatSelect;
  public groups: Group[] = [];
  public application!: Application;
  public form!: FormGroup;
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
    private readonly ngZone: NgZone,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.applicationService
      .getById(this.activatedRoute.snapshot.params.applicationId)
      .pipe(
        tap(application => {
          this.application = application;
          this.isReadonly = application.origin === 'KUBERNETES';
        }),
        switchMap(() => this.loadInitialGroups()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private updatePagination(pagination: Pagination): void {
    if (!pagination) return;
    this.totalPages = pagination.pageCount;
    if (pagination.page >= this.totalPages) {
      this.hasMoreGroups = false;
    } else {
      this.page = pagination.page + 1;
    }
  }

  private mergeGroups(newGroups: Group[]): void {
    const selectedIds = new Set(this.application.groups ?? []);
    const existingIds = new Set(this.groups.map(g => g.id));
    const filteredNewGroups = newGroups.filter(g => !existingIds.has(g.id));

    const selectedGroups: Group[] = [];
    const unselectedGroups: Group[] = [];

    for (const group of this.groups) {
      (selectedIds.has(group.id) ? selectedGroups : unselectedGroups).push(group);
    }

    this.groups = [...selectedGroups, ...unselectedGroups, ...filteredNewGroups];
  }

  private loadInitialGroups(): Observable<Group[]> {
    this.isLoading = true;

    const selectedGroupIds = this.application.groups ?? [];
    return combineLatest([
      this.groupv2Service.list(1, this.pageSize),
      selectedGroupIds.length > 0 ? this.groupv2Service.listById(selectedGroupIds, 1, selectedGroupIds.length) : of([]),
    ]).pipe(
      map(([page1Res, selectedRes]: any) => {
        const page1Groups: Group[] = page1Res?.data ?? [];
        const selectedGroups: Group[] = selectedRes?.data ?? [];

        this.groups = [...selectedGroups];
        this.mergeGroups(page1Groups);
        this.updatePagination(page1Res?.pagination);

        const userGroupList: Group[] = this.groups.filter(g => this.application.groups?.includes(g.id));
        this.form = new FormGroup({
          selectedGroups: new FormControl(userGroupList.map(g => g.id)),
        });

        if (this.isReadonly) {
          this.form.disable({ emitEvent: false });
        }

        this.initialFormValue = this.form.getRawValue();

        return this.groups;
      }),
      tap(() => (this.isLoading = false)),
      takeUntil(this.unsubscribe$),
    );
  }

  private loadGroups(page: number): Observable<Group[]> {
    if (this.isLoading || !this.hasMoreGroups) {
      return of([]);
    }

    this.isLoading = true;

    return this.groupv2Service.list(page, this.pageSize).pipe(
      tap((res: any) => {
        this.mergeGroups(res?.data ?? []);
        this.updatePagination(res?.pagination);
        this.isLoading = false;
      }),
      map((res: any) => res?.data ?? []),
      catchError(() => {
        this.isLoading = false;
        return of([]);
      }),
      takeUntil(this.unsubscribe$),
    );
  }

  onSelectToggle(opened: boolean): void {
    if (opened) {
      this.ngZone.onStable.pipe(take(1)).subscribe(() => {
        this.cdr.detectChanges();

        const panelElement = this.groupMatSelect?.panel?.nativeElement as HTMLElement | null;
        if (!panelElement) {
          return;
        }

        this.scrollContainer = panelElement;
        const boundScrollHandler = this.onScroll.bind(this);
        panelElement.addEventListener('scroll', boundScrollHandler);
        this.scrollListener = () => panelElement.removeEventListener('scroll', boundScrollHandler);
      });
    } else {
      this.cleanupScrollListener();
    }
  }

  private onScroll(event: Event): void {
    const target = event.target as HTMLElement;
    const threshold = 0.7 * target.scrollHeight;

    if (target.scrollTop + target.clientHeight >= threshold && this.hasMoreGroups && !this.isLoading) {
      this.loadGroups(this.page).subscribe();
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
