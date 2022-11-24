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
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { NewFile } from '@gravitee/ui-particles-angular';
import { StateService } from '@uirouter/angular';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';

import {
  ApiPortalDetailsDuplicateDialogComponent,
  ApiPortalDetailsDuplicateDialogData,
} from './api-portal-details-duplicate-dialog/api-portal-details-duplicate-dialog.component';
import {
  ApiPortalDetailsExportDialogComponent,
  ApiPortalDetailsExportDialogData,
} from './api-portal-details-export-dialog/api-portal-details-export-dialog.component';
import {
  ApiPortalDetailsPromoteDialogComponent,
  ApiPortalDetailsPromoteDialogData,
} from './api-portal-details-promote-dialog/api-portal-details-promote-dialog.component';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Api } from '../../../../entities/api';
import { Category } from '../../../../entities/category/Category';
import { Constants } from '../../../../entities/Constants';
import { ApiService } from '../../../../services-ngx/api.service';
import { CategoryService } from '../../../../services-ngx/category.service';
import { PolicyService } from '../../../../services-ngx/policy.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import {
  GioApiImportDialogComponent,
  GioApiImportDialogData,
} from '../../../../shared/components/gio-api-import-dialog/gio-api-import-dialog.component';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-portal-details',
  template: require('./api-portal-details.component.html'),
  styles: [require('./api-portal-details.component.scss')],
})
export class ApiPortalDetailsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public apiId: string;
  public api: Api;

  public apiDetailsForm: FormGroup;
  public initialApiDetailsFormValue: unknown;
  public labelsAutocompleteOptions: string[] = [];
  public apiCategories: Category[] = [];
  public apiOwner: string;
  public apiCreatedAt: number;
  public apiLastDeploymentAt: number;
  public dangerActions = {
    canAskForReview: false,
    canStartApi: false,
    canStopApi: false,
    canChangeApiLifecycle: false,
    canPublish: false,
    canUnpublish: false,
    canChangeVisibilityToPublic: false,
    canChangeVisibilityToPrivate: false,
    canDeprecate: false,
    canDelete: false,
  };
  public canPromote = false;
  public canDisplayJupiterToggle = false;

  public isQualityEnabled = false;

  public isReadOnly = false;
  public updateState: 'TO_UPDATE' | 'IN_PROGRESS' | 'UPDATED' | undefined;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiService,
    private readonly policyService: PolicyService,
    private readonly categoryService: CategoryService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.labelsAutocompleteOptions = this.constants.env?.settings?.api?.labelsDictionary ?? [];
    this.canDisplayJupiterToggle = this.constants.org?.settings?.jupiterMode?.enabled ?? false;

    this.isQualityEnabled = this.constants.env?.settings?.apiQualityMetrics?.enabled;

    combineLatest([this.apiService.get(this.ajsStateParams.apiId), this.categoryService.list()])
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap(([api, categories]) =>
          combineLatest([isImgUrl(api.picture_url), isImgUrl(api.background_url)]).pipe(
            map(
              ([hasPictureImg, hasBackgroundImg]) =>
                [
                  {
                    ...api,
                    picture_url: hasPictureImg ? api.picture_url : null,
                    background_url: hasBackgroundImg ? api.background_url : null,
                  },
                  categories,
                ] as const,
            ),
          ),
        ),
        tap(([api, categories]) => {
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || api.definition_context?.origin === 'kubernetes';

          this.apiId = api.id;
          this.api = api;
          this.updateState = api.gravitee == null || api.gravitee === '1.0.0' ? 'TO_UPDATE' : undefined;

          this.apiCategories = categories;
          this.apiOwner = api.owner.displayName;
          this.apiCreatedAt = api.created_at;
          this.apiLastDeploymentAt = api.updated_at;

          this.dangerActions = {
            canAskForReview:
              this.constants.env?.settings?.apiReview?.enabled &&
              (api.workflow_state === 'DRAFT' || api.workflow_state === 'REQUEST_FOR_CHANGES' || !api.workflow_state),
            canStartApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflow_state || api.workflow_state === 'REVIEW_OK'))) &&
              api.state === 'STOPPED',
            canStopApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflow_state || api.workflow_state === 'REVIEW_OK'))) &&
              api.state === 'STARTED',

            canChangeApiLifecycle: this.canChangeApiLifecycle(api),
            canPublish: !api.lifecycle_state || api.lifecycle_state === 'CREATED' || api.lifecycle_state === 'UNPUBLISHED',
            canUnpublish: api.lifecycle_state === 'PUBLISHED',

            canChangeVisibilityToPublic: api.lifecycle_state !== 'DEPRECATED' && api.visibility === 'PRIVATE',
            canChangeVisibilityToPrivate: api.lifecycle_state !== 'DEPRECATED' && api.visibility === 'PUBLIC',
            canDeprecate: api.lifecycle_state !== 'DEPRECATED',
            canDelete: !(api.state === 'STARTED' || api.lifecycle_state === 'PUBLISHED'),
          };

          this.canPromote = this.dangerActions.canChangeApiLifecycle && api.lifecycle_state !== 'DEPRECATED';

          this.apiDetailsForm = new FormGroup({
            name: new FormControl(
              {
                value: api.name,
                disabled: this.isReadOnly,
              },
              [Validators.required],
            ),
            version: new FormControl(
              {
                value: api.version,
                disabled: this.isReadOnly,
              },
              [Validators.required, this.apiService.versionValidator()],
            ),
            description: new FormControl(
              {
                value: api.description,
                disabled: this.isReadOnly,
              },
              [Validators.required],
            ),
            picture: new FormControl({
              value: api.picture_url ? [api.picture_url] : [],
              disabled: this.isReadOnly,
            }),
            background: new FormControl({
              value: api.background_url ? [api.background_url] : [],
              disabled: this.isReadOnly,
            }),
            labels: new FormControl({
              value: api.labels,
              disabled: this.isReadOnly,
            }),
            categories: new FormControl({
              value: api.categories,
              disabled: this.isReadOnly,
            }),
            enableJupiter: new FormControl({
              value: api.execution_mode === 'jupiter',
              disabled: this.isReadOnly,
            }),
          });

          this.initialApiDetailsFormValue = this.apiDetailsForm.getRawValue();
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    const apiDetailsFormValue = this.apiDetailsForm.getRawValue();

    const picture = getBase64(apiDetailsFormValue.picture[0]);
    const background = getBase64(apiDetailsFormValue.background[0]);

    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        map((api) => ({
          ...api,
          ...(picture !== null ? { picture: picture } : { picture_url: null, picture: null }),
          ...(background !== null ? { background: background } : { background_url: null, background: null }),
          name: apiDetailsFormValue.name,
          version: apiDetailsFormValue.version,
          description: apiDetailsFormValue.description,
          labels: apiDetailsFormValue.labels,
          categories: apiDetailsFormValue.categories,
          ...(this.canDisplayJupiterToggle
            ? { execution_mode: apiDetailsFormValue.enableJupiter ? ('jupiter' as const) : ('v3' as const) }
            : {}),
        })),
        switchMap((api) => this.apiService.update(api)),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        tap(() => {
          this.apiId = undefined; // force to reload quality metrics
          this.ngOnInit();
        }),
      )
      .subscribe();
  }

  importApi() {
    this.policyService
      .listSwaggerPolicies()
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((policies) =>
          this.matDialog
            .open<GioApiImportDialogComponent, GioApiImportDialogData>(GioApiImportDialogComponent, {
              data: {
                apiId: this.ajsStateParams.apiId,
                definitionVersion: this.api.gravitee,
                policies,
              },
              role: 'alertdialog',
              id: 'importApiDialog',
            })
            .afterClosed(),
        ),
        filter((confirm) => confirm === true),
        tap(() => this.ngOnInit()),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? 'An error occurred while importing the API.');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  duplicateApi() {
    this.matDialog
      .open<ApiPortalDetailsDuplicateDialogComponent, ApiPortalDetailsDuplicateDialogData>(ApiPortalDetailsDuplicateDialogComponent, {
        data: {
          api: this.api,
        },
        role: 'alertdialog',
        id: 'duplicateApiDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((apiDuplicated) => !!apiDuplicated),
        tap((apiDuplicated) => this.ajsState.go('management.apis.detail.portal.general', { apiId: apiDuplicated.id })),
      )
      .subscribe();
  }

  exportApi() {
    this.matDialog
      .open<ApiPortalDetailsExportDialogComponent, ApiPortalDetailsExportDialogData>(ApiPortalDetailsExportDialogComponent, {
        data: {
          api: this.api,
        },
        role: 'alertdialog',
        id: 'exportApiDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }

  promoteApi() {
    this.matDialog
      .open<ApiPortalDetailsPromoteDialogComponent, ApiPortalDetailsPromoteDialogData>(ApiPortalDetailsPromoteDialogComponent, {
        data: {
          api: this.api,
        },
        role: 'alertdialog',
        id: 'promoteApiDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }

  private canChangeApiLifecycle(api: Api): boolean {
    if (this.constants.env?.settings?.apiReview?.enabled) {
      return !api.workflow_state || api.workflow_state === 'REVIEW_OK';
    } else {
      return api.lifecycle_state === 'CREATED' || api.lifecycle_state === 'PUBLISHED' || api.lifecycle_state === 'UNPUBLISHED';
    }
  }

  public updateApiVersion() {
    this.updateState = 'IN_PROGRESS';
    this.apiService
      .migrateApiToPolicyStudio(this.apiId)
      .pipe(
        tap(() => {
          this.updateState = 'UPDATED';
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          this.updateState = 'TO_UPDATE';
          return EMPTY;
        }),
      )
      .subscribe();
  }

  public reloadApi() {
    this.ajsState.go('management.apis.detail.portal.general', { apiId: this.apiId }, { reload: true });
  }
}

const isImgUrl = (url: string): Promise<boolean> => {
  const img = new Image();
  img.src = url;
  return new Promise((resolve) => {
    img.onerror = () => resolve(false);
    img.onload = () => resolve(true);
  });
};

function getBase64(file?: NewFile | string): string | undefined | null {
  if (!file) {
    // If no file, return null to remove it
    return null;
  }
  if (!(file instanceof NewFile)) {
    // If file not changed, return undefined to keep it
    return undefined;
  }

  return file.dataUrl;
}
