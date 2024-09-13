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
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, EMPTY, of, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { NewFile } from '@gravitee/ui-particles-angular';

import {
  ApiGeneralInfoDuplicateDialogComponent,
  ApiPortalDetailsDuplicateDialogData,
} from './api-general-info-duplicate-dialog/api-general-info-duplicate-dialog.component';
import {
  ApiGeneralInfoExportV2DialogComponent,
  ApiPortalDetailsExportV2DialogData,
} from './api-general-info-export-v2-dialog/api-general-info-export-v2-dialog.component';
import {
  ApiGeneralInfoPromoteDialogComponent,
  ApiPortalDetailsPromoteDialogData,
} from './api-general-info-promote-dialog/api-general-info-promote-dialog.component';
import {
  ApiGeneralInfoExportV4DialogComponent,
  ApiGeneralDetailsExportV4DialogData,
  ApiGeneralDetailsExportV4DialogResult,
} from './api-general-info-export-v4-dialog/api-general-info-export-v4-dialog.component';

import { Category } from '../../../entities/category/Category';
import { Constants } from '../../../entities/Constants';
import { CategoryService } from '../../../services-ngx/category.service';
import { PolicyService } from '../../../services-ngx/policy.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioApiImportDialogComponent, GioApiImportDialogData } from '../component/gio-api-import-dialog/gio-api-import-dialog.component';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api, ApiV2, ApiV4, UpdateApi, UpdateApiV2, UpdateApiV4 } from '../../../entities/management-api-v2';
import { Integration } from '../../integrations/integrations.model';
import { IntegrationsService } from '../../../services-ngx/integrations.service';

@Component({
  selector: 'api-general-info',
  templateUrl: './api-general-info.component.html',
  styleUrls: ['./api-general-info.component.scss'],
})
export class ApiGeneralInfoComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public apiId: string;
  public api: Api;

  public apiDetailsForm: UntypedFormGroup;
  public apiImagesForm: UntypedFormGroup;
  public parentForm: UntypedFormGroup;
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
  public cannotPromote = true;
  public canDisplayV4EmulationEngineToggle = false;

  public isQualityEnabled = false;
  public isQualitySupported = false;

  public isReadOnly = false;
  public isKubernetesOrigin = false;

  public integrationName = '';
  public integrationId = '';

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly policyService: PolicyService,
    private readonly categoryService: CategoryService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly integrationsService: IntegrationsService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.labelsAutocompleteOptions = this.constants.env?.settings?.api?.labelsDictionary ?? [];

    this.isQualityEnabled = this.constants.env?.settings?.apiQualityMetrics?.enabled;

    this.activatedRoute.params
      .pipe(
        switchMap((params) => {
          this.apiId = params.apiId;
          return combineLatest([this.apiService.get(this.apiId), this.categoryService.list()]);
        }),
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
          this.isKubernetesOrigin = api.originContext?.origin === 'KUBERNETES';
          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-definition-u']) || this.isKubernetesOrigin || api.definitionVersion === 'V1';

          this.api = api;

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
          this.cannotPromote =
            !(this.dangerActions.canChangeApiLifecycle && api.lifecycleState !== 'DEPRECATED') ||
            this.isKubernetesOrigin ||
            api.definitionVersion === 'V4' ||
            api.definitionVersion === 'V1';

          this.apiDetailsForm = new UntypedFormGroup({
            name: new UntypedFormControl(
              {
                value: api.name,
                disabled: this.isReadOnly,
              },
              [Validators.required],
            ),
            version: new UntypedFormControl(
              {
                value: api.apiVersion,
                disabled: this.isReadOnly,
              },
              [Validators.required],
            ),
            description: new UntypedFormControl({
              value: api.description,
              disabled: this.isReadOnly,
            }),
            labels: new UntypedFormControl({
              value: api.labels,
              disabled: this.isReadOnly,
            }),
            categories: new UntypedFormControl({
              value: api.categories,
              disabled: this.isReadOnly,
            }),
            emulateV4Engine: new UntypedFormControl({
              value: api.definitionVersion === 'V2' && (api as ApiV2).executionMode === 'V4_EMULATION_ENGINE',
              disabled: this.isReadOnly,
            }),
          });
          this.apiImagesForm = new UntypedFormGroup({
            picture: new UntypedFormControl({
              value: api._links['pictureUrl'] ? [api._links['pictureUrl']] : [],
              disabled: this.isReadOnly,
            }),
            background: new UntypedFormControl({
              value: api._links['backgroundUrl'] ? [api._links['backgroundUrl']] : [],
              disabled: this.isReadOnly,
            }),
          });
          this.parentForm = new UntypedFormGroup({
            details: this.apiDetailsForm,
            images: this.apiImagesForm,
          });

          this.initialApiDetailsFormValue = this.parentForm.getRawValue();
          this.isQualitySupported = this.api.definitionVersion === 'V2' || this.api.definitionVersion === 'V1';
        }),
        switchMap(([api]) => {
          if ('integrationId' in api.originContext) {
            return this.integrationsService.getIntegration(api.originContext.integrationId);
          }
          return of(null);
        }),
        tap((integration: Integration | null) => {
          if (integration) {
            this.integrationName = integration.name;
            this.integrationId = integration.id;
          }
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
      .get(this.apiId)
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
                apiId: this.apiId,
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
      .open<ApiGeneralInfoDuplicateDialogComponent, ApiPortalDetailsDuplicateDialogData>(ApiGeneralInfoDuplicateDialogComponent, {
        data: {
          api: this.api,
        },
        role: 'alertdialog',
        id: 'duplicateApiDialog',
      })
      .afterClosed()
      .pipe(
        filter((apiDuplicated) => !!apiDuplicated),
        switchMap((apiDuplicated) => this.router.navigate(['../', apiDuplicated.id], { relativeTo: this.activatedRoute })),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  exportApi() {
    const exportDialog$ =
      this.api.definitionVersion === 'V4'
        ? this.matDialog
            .open<ApiGeneralInfoExportV4DialogComponent, ApiGeneralDetailsExportV4DialogData, ApiGeneralDetailsExportV4DialogResult>(
              ApiGeneralInfoExportV4DialogComponent,
              {
                data: {
                  api: this.api,
                },
                role: 'alertdialog',
                id: 'exportApiDialog',
              },
            )
            .afterClosed()
        : this.matDialog
            .open<ApiGeneralInfoExportV2DialogComponent, ApiPortalDetailsExportV2DialogData>(ApiGeneralInfoExportV2DialogComponent, {
              data: {
                api: this.api,
              },
              role: 'alertdialog',
              id: 'exportApiDialog',
            })
            .afterClosed();

    exportDialog$.pipe(takeUntil(this.unsubscribe$)).subscribe();
  }

  promoteApi() {
    if (this.api.definitionVersion === 'V2')
      this.matDialog
        .open<ApiGeneralInfoPromoteDialogComponent, ApiPortalDetailsPromoteDialogData>(ApiGeneralInfoPromoteDialogComponent, {
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
