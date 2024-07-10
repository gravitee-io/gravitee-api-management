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
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiDocumentationV2Service } from '../../../../services-ngx/api-documentation-v2.service';
import {
  Breadcrumb,
  getLogoForPageType,
  Page,
  CreateDocumentation,
  CreateDocumentationType,
  PageType,
  Api,
  getTooltipForPageType,
  AccessControl,
  Visibility,
  Group,
  EditDocumentation,
  PageSource,
} from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { GroupV2Service } from '../../../../services-ngx/group-v2.service';
import { FetcherService } from '../../../../services-ngx/fetcher.service';

interface EditPageForm {
  stepOne: FormGroup<{
    name: FormControl<string>;
    visibility: FormControl<Visibility>;
    accessControlGroups: FormControl<string[]>;
    excludeGroups: FormControl<boolean>;
  }>;
  content: FormControl<string>;
  source: FormControl<string>;
  sourceConfiguration: FormControl<undefined | unknown>;
}

@Component({
  selector: 'api-documentation-edit-page',
  templateUrl: './api-documentation-v4-edit-page.component.html',
  styleUrls: ['./api-documentation-v4-edit-page.component.scss'],
})
export class ApiDocumentationV4EditPageComponent implements OnInit, OnDestroy {
  form: FormGroup<EditPageForm>;
  mode: 'create' | 'edit';
  pageTitle = 'Add new page';
  exitLabel = 'Exit without saving';
  pageType: PageType;
  step3Title: string;
  source: 'FILL' | 'IMPORT' | 'HTTP' = 'FILL';
  breadcrumbs: Breadcrumb[];

  api: Api;
  formUnchanged: boolean;
  page: Page;
  iconUrl: string;
  iconTooltip: string;
  isReadOnly: boolean = false;
  groups: Group[];
  schema$: Observable<any>;

  private existingNames: string[] = [];
  private initialAccessControlGroups: string[] = [];
  private unsubscribe$: Subject<void> = new Subject<void>();
  private readonly httpFetcherName = 'http-fetcher';

  private readonly httpValue = 'HTTP';

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
    private readonly groupService: GroupV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly fetcherService: FetcherService,
  ) {}

  ngOnInit(): void {
    this.form = new FormGroup<EditPageForm>({
      stepOne: new FormGroup({
        name: new FormControl<string>('', [Validators.required, this.pageNameUniqueValidator()]),
        visibility: new FormControl<Visibility>('PUBLIC', [Validators.required]),
        accessControlGroups: new FormControl<string[]>([]),
        excludeGroups: new FormControl<boolean>({ value: false, disabled: true }),
      }),
      content: new FormControl<string>('', [Validators.required]),
      source: new FormControl<string>(this.source, [Validators.required]),
      sourceConfiguration: new FormControl<undefined | unknown>({}),
    });

    this.form.controls.stepOne.controls.accessControlGroups.valueChanges
      .pipe(
        tap((value) => {
          if (value.length === 0) {
            this.form.controls.stepOne.controls.excludeGroups.setValue(false);
            this.form.controls.stepOne.controls.excludeGroups.disable();
          } else {
            this.form.controls.stepOne.controls.excludeGroups.enable();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    if (this.activatedRoute.snapshot.params.pageId) {
      this.mode = 'edit';
      this.step3Title = 'Edit content';
      this.source = 'FILL';

      this.form.valueChanges
        .pipe(
          tap((value) => {
            if (this.page) {
              this.formUnchanged =
                this.page.name === value.stepOne?.name &&
                this.page.visibility === value.stepOne?.visibility &&
                this.source === value.source &&
                this.page.content === value.content &&
                (this.page.excludedAccessControls === undefined || this.page.excludedAccessControls === value.stepOne.excludeGroups) &&
                this.initialAccessControlGroups === value.stepOne.accessControlGroups;
            }
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();

      if (!this.permissionService.hasAnyMatching(['api-documentation-u'])) {
        this.form.disable();
        this.exitLabel = 'Exit';
      }
    } else {
      this.mode = 'create';
      this.step3Title = this.source === 'IMPORT' ? 'Upload a file' : 'Add content';
      this.pageType = this.activatedRoute.snapshot.queryParams.pageType;
    }

    this.apiV2Service
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api) => {
          this.api = api;
          this.isReadOnly = api.originContext?.origin === 'KUBERNETES';
          return this.mode === 'edit' ? this.loadEditPage() : of({});
        }),
        switchMap((_) =>
          combineLatest([this.apiDocumentationService.getApiPages(this.api.id, this.getParentId()), this.groupService.list(1, 999)]),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: ([pagesResponse, groupsResponse]) => {
          this.iconUrl = getLogoForPageType(this.pageType);
          this.iconTooltip = getTooltipForPageType(this.pageType);

          this.breadcrumbs = pagesResponse.breadcrumb;
          this.existingNames = pagesResponse.pages
            .filter((page) => page.id !== this.page?.id && page.type === this.pageType)
            .map((page) => page.name.toLowerCase().trim());

          this.groups = groupsResponse?.data ?? [];
        },
      });

    this.schema$ = this.fetcherService.getList().pipe(
      map((list) => list.find((fetcher) => fetcher.id === this.httpFetcherName)?.schema),
      map((schema) => JSON.parse(schema)),
    );
    this.form.controls.stepOne.controls.name.valueChanges
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => (this.pageTitle = value || 'Add new page'));
    this.form.controls.source.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((value) => {
      if (value === this.httpValue) {
        this.form.controls.content.clearValidators();
      } else {
        this.form.controls.content.addValidators([Validators.required]);
      }
      this.form.controls.content.updateValueAndValidity();
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  create() {
    this.createPage()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => {
        this.goBackToPageList();
      });
  }

  createAndPublish() {
    this.createPage()
      .pipe(
        switchMap((page) => this.apiDocumentationService.publishDocumentationPage(this.api.id, page.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.goBackToPageList();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Cannot publish page');
        },
      });
  }

  update() {
    this.updatePage()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: () => {
          this.goBackToPageList();
        },
      });
  }

  updateAndPublish() {
    this.updatePage()
      .pipe(
        switchMap((page) => this.apiDocumentationService.publishDocumentationPage(this.api.id, page.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: () => {
          this.goBackToPageList();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Cannot publish page');
        },
      });
  }

  private updatePage(): Observable<Page> {
    const formValue = this.form.getRawValue();
    return this.apiDocumentationService.getApiPage(this.api.id, this.activatedRoute.snapshot.params.pageId).pipe(
      switchMap((page) => {
        const nonGroupAccessControls = page.accessControls ? page.accessControls.filter((ac) => ac.referenceType !== 'GROUP') : [];
        const selectedGroupAccessControls: AccessControl[] = formValue.stepOne.accessControlGroups.map((referenceId) => ({
          referenceId,
          referenceType: 'GROUP',
        }));

        const updateDocumentation: EditDocumentation = {
          ...page,
          name: formValue.stepOne.name,
          visibility: formValue.stepOne.visibility,
          content: formValue.content,
          excludedAccessControls: formValue.stepOne.excludeGroups,
          accessControls: [...nonGroupAccessControls, ...selectedGroupAccessControls],
          ...(formValue.source === this.httpValue && {
            source: this.obtainSource(formValue.source, formValue.sourceConfiguration),
          }),
        };
        return this.apiDocumentationService.updateDocumentationPage(
          this.api.id,
          this.activatedRoute.snapshot.params.pageId,
          updateDocumentation,
        );
      }),
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot update page');
        return EMPTY;
      }),
    );
  }

  private obtainSource(sourceType: string, configuration: unknown | undefined): PageSource {
    if (sourceType === this.httpValue) {
      return {
        type: this.httpFetcherName,
        configuration,
      };
    }
  }

  goBackToPageList() {
    this.router.navigate(['../'], {
      relativeTo: this.activatedRoute,
      queryParams: { parentId: this.getParentId() },
    });
  }

  deletePage() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        data: {
          title: `Delete your page`,
          content: `Are you sure you want to delete this page? This action is irreversible.`,
          confirmButton: 'Delete',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => !!confirmed),
        switchMap((_) => this.apiDocumentationService.deleteDocumentationPage(this.activatedRoute.snapshot.params.apiId, this.page?.id)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.snackBarService.success('Page deleted successfully');
          this.goBackToPageList();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Error while deleting page');
        },
      });
  }

  private createPage(): Observable<Page> {
    const formValue = this.form.getRawValue();
    // Only Markdown, Swagger, and AsyncAPI pages can be created
    if (this.pageType !== 'MARKDOWN' && this.pageType !== 'SWAGGER' && this.pageType !== 'ASYNCAPI') {
      this.snackBarService.error(`Cannot create page with type [${this.pageType}]`);
      return;
    }
    const createPage: CreateDocumentation = {
      type: this.pageType as CreateDocumentationType,
      name: formValue.stepOne.name,
      visibility: formValue.stepOne.visibility,
      content: formValue.content,
      parentId: this.activatedRoute.snapshot.queryParams.parentId || 'ROOT',
      accessControls: formValue.stepOne.accessControlGroups.map((referenceId) => ({ referenceId, referenceType: 'GROUP' })),
      excludedAccessControls: formValue.stepOne.excludeGroups,
      ...(formValue.source === this.httpValue && {
        source: this.obtainSource(formValue.source, formValue.sourceConfiguration),
      }),
    };
    return this.apiDocumentationService.createDocumentationPage(this.api.id, createPage).pipe(
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot save page');
        return EMPTY;
      }),
    );
  }

  private loadEditPage(): Observable<Page> {
    return this.apiDocumentationService.getApiPage(this.api.id, this.activatedRoute.snapshot.params.pageId).pipe(
      tap((page) => {
        this.pageTitle = page.name;
        this.page = page;
        this.pageType = page.type;

        this.form.controls.stepOne.controls.name.setValue(this.page.name);
        this.form.controls.stepOne.controls.visibility.setValue(this.page.visibility);
        if (this.page.accessControls) {
          this.form.controls.stepOne.controls.accessControlGroups.setValue(
            this.page.accessControls.filter((ac) => ac.referenceType === 'GROUP').map((ac) => ac.referenceId),
          );
        }
        this.initialAccessControlGroups = this.form.value.stepOne.accessControlGroups;
        this.form.controls.stepOne.controls.excludeGroups.setValue(this.page.excludedAccessControls === true);
        if (this.initialAccessControlGroups.length === 0) {
          this.form.controls.stepOne.controls.excludeGroups.disable();
        } else {
          this.form.controls.stepOne.controls.excludeGroups.enable();
        }
        this.form.controls.content.setValue(this.page.content);
      }),
    );
  }

  private pageNameUniqueValidator(): ValidatorFn {
    return (nameControl: AbstractControl): ValidationErrors | null =>
      this.existingNames.includes(nameControl.value?.toLowerCase().trim()) ? { unique: true } : null;
  }

  private getParentId(): string {
    return this.activatedRoute.snapshot.queryParams.parentId ?? this.page?.parentId ?? 'ROOT';
  }
}
