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
import { StateService } from '@uirouter/angular';
import { combineLatest, EMPTY, of, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { NewFile } from '@gravitee/ui-particles-angular';

import {
  ApiPortalDetailsDuplicateDialogComponent,
  ApiPortalDetailsDuplicateDialogData,
} from './api-portal-details-duplicate-dialog/api-portal-details-duplicate-dialog.component';
import {
  ApiPortalDetailsExportDialogComponent,
  ApiPortalDetailsExportDialogData,
  buildFileName,
} from './api-portal-details-export-dialog/api-portal-details-export-dialog.component';
import {
  ApiPortalDetailsPromoteDialogComponent,
  ApiPortalDetailsPromoteDialogData,
} from './api-portal-details-promote-dialog/api-portal-details-promote-dialog.component';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Category } from '../../../../entities/category/Category';
import { Constants } from '../../../../entities/Constants';
import { CategoryService } from '../../../../services-ngx/category.service';
import { PolicyService } from '../../../../services-ngx/policy.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import {
  GioApiImportDialogComponent,
  GioApiImportDialogData,
} from '../../../../shared/components/gio-api-import-dialog/gio-api-import-dialog.component';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api, ApiV2, ApiV4, UpdateApi, UpdateApiV2, UpdateApiV4 } from '../../../../entities/management-api-v2';
import { ApiService } from '../../../../services-ngx/api.service';

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
  public apiImagesForm: FormGroup;
  public parentForm: FormGroup;
  public initialApiDetailsFormValue: unknown;
  public labelsAutocompleteOptions: string[] = [];
  public apiCategories: Category[] = [];
  public apiOwner: string;
  public apiCreatedAt: Date;
  public apiLastDeploymentAt: Date;
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
  public canDisplayV4EmulationEngineToggle = false;

  public isQualityEnabled = false;

  public isReadOnly = false;
  public isKubernetesOrigin = false;
  public updateState: 'TO_UPDATE' | 'IN_PROGRESS' | 'UPDATED' | undefined;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly legacyApiService: ApiService,
    private readonly apiService: ApiV2Service,
    private readonly policyService: PolicyService,
    private readonly categoryService: CategoryService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.labelsAutocompleteOptions = this.constants.env?.settings?.api?.labelsDictionary ?? [];

    this.isQualityEnabled = this.constants.env?.settings?.apiQualityMetrics?.enabled;

    combineLatest([this.apiService.get(this.ajsStateParams.apiId), this.categoryService.list()])
      .pipe(
        switchMap(([api, categories]) =>
          combineLatest([isImgUrl(api._links['pictureUrl']), isImgUrl(api._links['backgroundUrl'])]).pipe(
            map(
              ([hasPictureImg, hasBackgroundImg]) =>
                // FIXME:create type ApiVM?
                [
                  {
                    ...api,
                    _links: {
                      ...api._links,
                      pictureUrl: hasPictureImg ? api._links['pictureUrl'] : null,
                      backgroundUrl: hasBackgroundImg ? api._links['backgroundUrl'] : null,
                    },
                  },
                  categories,
                ] as const,
            ),
          ),
        ),
        tap(([api, categories]) => {
          this.isKubernetesOrigin = api.definitionContext?.origin === 'KUBERNETES';
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || this.isKubernetesOrigin;

          this.apiId = api.id;
          this.api = api;
          this.updateState = api.definitionVersion == null || api.definitionVersion === 'V1' ? 'TO_UPDATE' : undefined;

          this.apiCategories = categories;
          this.apiOwner = api.primaryOwner.displayName;
          this.apiCreatedAt = api.createdAt;
          this.apiLastDeploymentAt = api.updatedAt;

          this.dangerActions = {
            canAskForReview:
              this.constants.env?.settings?.apiReview?.enabled &&
              (api.workflowState === 'DRAFT' || api.workflowState === 'REQUEST_FOR_CHANGES' || !api.workflowState),
            canStartApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflowState || api.workflowState === 'REVIEW_OK'))) &&
              api.state === 'STOPPED',
            canStopApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflowState || api.workflowState === 'REVIEW_OK'))) &&
              api.state === 'STARTED',

            canChangeApiLifecycle: this.canChangeApiLifecycle(api),
            canPublish: !api.lifecycleState || api.lifecycleState === 'CREATED' || api.lifecycleState === 'UNPUBLISHED',
            canUnpublish: api.lifecycleState === 'PUBLISHED',

            canChangeVisibilityToPublic: api.lifecycleState !== 'DEPRECATED' && api.visibility === 'PRIVATE',
            canChangeVisibilityToPrivate: api.lifecycleState !== 'DEPRECATED' && api.visibility === 'PUBLIC',
            canDeprecate: api.lifecycleState !== 'DEPRECATED',
            canDelete: !(api.state === 'STARTED' || api.lifecycleState === 'PUBLISHED'),
          };
          this.canDisplayV4EmulationEngineToggle = (api.definitionVersion != null && api.definitionVersion === 'V2') ?? false;
          this.canPromote = this.dangerActions.canChangeApiLifecycle && api.lifecycleState !== 'DEPRECATED';

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
                value: api.apiVersion,
                disabled: this.isReadOnly,
              },
              [Validators.required],
            ),
            description: new FormControl({
              value: api.description,
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
            emulateV4Engine: new FormControl({
              value: api.definitionVersion === 'V2' && (api as ApiV2).executionMode === 'V4_EMULATION_ENGINE',
              disabled: this.isReadOnly,
            }),
          });
          this.apiImagesForm = new FormGroup({
            picture: new FormControl({
              value: api._links['pictureUrl'] ? [api._links['pictureUrl']] : [],
              disabled: this.isReadOnly,
            }),
            background: new FormControl({
              value: api._links['backgroundUrl'] ? [api._links['backgroundUrl']] : [],
              disabled: this.isReadOnly,
            }),
          });
          this.parentForm = new FormGroup({
            details: this.apiDetailsForm,
            images: this.apiImagesForm,
          });

          this.initialApiDetailsFormValue = this.parentForm.getRawValue();
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
    const apiDetailsFormValue = this.apiDetailsForm.getRawValue();
    const apiImagesFormValue = this.apiImagesForm.getRawValue();

    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        map((api: Api) => {
          if (api.definitionVersion === 'V2') {
            const apiToUpdate: UpdateApiV2 = {
              ...(api as ApiV2),
              name: apiDetailsFormValue.name,
              apiVersion: apiDetailsFormValue.version,
              description: apiDetailsFormValue.description,
              labels: apiDetailsFormValue.labels,
              categories: apiDetailsFormValue.categories,
              ...(this.canDisplayV4EmulationEngineToggle
                ? { executionMode: apiDetailsFormValue.emulateV4Engine ? 'V4_EMULATION_ENGINE' : 'V3' }
                : {}),
            };
            return apiToUpdate;
          }
          const apiToUpdate: UpdateApiV4 = {
            ...(api as ApiV4),
            name: apiDetailsFormValue.name,
            apiVersion: apiDetailsFormValue.version,
            description: apiDetailsFormValue.description,
            labels: apiDetailsFormValue.labels,
            categories: apiDetailsFormValue.categories,
          };
          return apiToUpdate;
        }),
        switchMap((api: UpdateApi) => {
          if (this.apiDetailsForm.dirty) {
            return this.apiService.update(this.apiId, api);
          }
          return of(this.api);
        }),
        switchMap((api: Api) => {
          if (this.apiImagesForm.controls['picture'].dirty) {
            const picture = getBase64(apiImagesFormValue.picture[0]);
            if (picture) {
              return this.apiService.updatePicture(this.apiId, picture).pipe(switchMap(() => of(api)));
            }
            return this.apiService.deletePicture(this.apiId).pipe(switchMap(() => of(api)));
          }
          return of(api);
        }),
        switchMap((api: Api) => {
          if (this.apiImagesForm.controls['background'].dirty) {
            const background = getBase64(apiImagesFormValue.background[0]);
            if (background) {
              return this.apiService.updateBackground(this.apiId, background).pipe(switchMap(() => of(api)));
            }
            return this.apiService.deleteBackground(this.apiId).pipe(switchMap(() => of(api)));
          }
          return of(api);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        tap(() => {
          this.apiId = undefined; // force to reload quality metrics
          this.ngOnInit();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  importApi() {
    this.policyService
      .listSwaggerPolicies()
      .pipe(
        switchMap((policies) =>
          this.matDialog
            .open<GioApiImportDialogComponent, GioApiImportDialogData>(GioApiImportDialogComponent, {
              data: {
                apiId: this.ajsStateParams.apiId,
                definitionVersion: this.api.definitionVersion,
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
        takeUntil(this.unsubscribe$),
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
        filter((apiDuplicated) => !!apiDuplicated),
        tap((apiDuplicated) => this.ajsState.go('management.apis.detail.portal.general', { apiId: apiDuplicated.id })),

        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  exportApi() {
    if (this.api.definitionVersion === 'V4') {
      this.apiService
        .export(this.apiId)
        .pipe(
          tap((blob) => {
            const anchor = document.createElement('a');
            anchor.download = buildFileName(this.api);
            anchor.href = (window.webkitURL || window.URL).createObjectURL(blob);
            anchor.click();
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
    } else {
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
  }

  promoteApi() {
    if (this.api.definitionVersion === 'V2')
      this.matDialog
        .open<ApiPortalDetailsPromoteDialogComponent, ApiPortalDetailsPromoteDialogData>(ApiPortalDetailsPromoteDialogComponent, {
          data: {
            api: this.api as ApiV2,
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
      return !api.workflowState || api.workflowState === 'REVIEW_OK';
    } else {
      return api.lifecycleState === 'CREATED' || api.lifecycleState === 'PUBLISHED' || api.lifecycleState === 'UNPUBLISHED';
    }
  }

  public updateApiVersion() {
    this.updateState = 'IN_PROGRESS';
    this.legacyApiService
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
        takeUntil(this.unsubscribe$),
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
