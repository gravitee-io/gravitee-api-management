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
import { Input, Component, OnInit, signal, inject, DestroyRef } from '@angular/core';
import { AsyncPipe, NgOptimizedImage } from '@angular/common';
import {
  GioBannerModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormJsonSchemaModule,
  GioFormSelectionInlineModule,
  GioFormSlideToggleModule,
  GioJsonSchema,
} from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { combineLatest, EMPTY, Observable, of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { catchError, distinctUntilChanged, filter, map, switchMap, tap } from 'rxjs/operators';
import { MatTooltip } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEqual } from 'lodash';
import { MatTab, MatTabGroup } from '@angular/material/tabs';

import { ApiDocumentationV2Service } from '../../../../../services-ngx/api-documentation-v2.service';
import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiDocumentationV4VisibilityComponent } from '../api-documentation-v4-visibility/api-documentation-v4-visibility.component';
import { AccessControl, Api, Breadcrumb, EditDocumentation, Group, Page, Visibility } from '../../../../../entities/management-api-v2';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { ApiDocumentationV4ContentEditorComponent } from '../api-documentation-v4-content-editor/api-documentation-v4-content-editor.component';
import { ApiDocumentationV4FileUploadComponent } from '../api-documentation-v4-file-upload/api-documentation-v4-file-upload.component';
import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import {
  ApiDocumentationV4PageConfigurationComponent,
  PageConfigurationForm,
} from '../api-documentation-v4-page-configuration/api-documentation-v4-page-configuration.component';
import { ApiDocumentationV4PageHeaderComponent } from '../api-documentation-v4-page-header/api-documentation-v4-page-header.component';
import { FetcherService } from '../../../../../services-ngx/fetcher.service';

interface OpenApiConfiguration {
  entrypointAsBasePath: FormControl<boolean>;
  entrypointsAsServers: FormControl<boolean>;
  viewer: FormControl<string>;
  tryItURL: FormControl<string>;
  tryIt: FormControl<boolean>;
  tryItAnonymous: FormControl<boolean>;
  showURL: FormControl<boolean>;
  displayOperationId: FormControl<boolean>;
  usePkce: FormControl<boolean>;
  docExpansion: FormControl<string>;
  enableFiltering: FormControl<boolean>;
  showExtensions: FormControl<boolean>;
  showCommonExtensions: FormControl<boolean>;
  maxDisplayedTags: FormControl<number>;
}

interface EditPageForm {
  pageConfiguration: FormGroup<PageConfigurationForm>;
  content: FormControl<string>;
  openApiConfiguration: FormGroup<OpenApiConfiguration>;
  sourceConfiguration: FormControl<unknown>;
}

@Component({
  selector: 'documentation-edit-page',
  standalone: true,
  imports: [
    AsyncPipe,
    GioFormJsonSchemaModule,
    GioFormSelectionInlineModule,
    GioFormSlideToggleModule,
    GioPermissionModule,
    MatButton,
    MatCard,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatOption,
    MatSelect,
    MatSlideToggle,
    NgOptimizedImage,
    ReactiveFormsModule,
    ApiDocumentationV4VisibilityComponent,
    MatTooltip,
    ApiDocumentationV4ContentEditorComponent,
    ApiDocumentationV4FileUploadComponent,
    ApiDocumentationV4Module,
    ApiDocumentationV4PageConfigurationComponent,
    MatTabGroup,
    MatTab,
    MatCardContent,
    ApiDocumentationV4PageHeaderComponent,
    GioBannerModule,
  ],
  templateUrl: './documentation-edit-page.component.html',
  styleUrl: './documentation-edit-page.component.scss',
})
export class DocumentationEditPageComponent implements OnInit {
  @Input()
  goBackRouterLink: string[];

  @Input()
  api: Api;

  @Input()
  page: Page;

  form: FormGroup<EditPageForm>;
  source: 'FILL' | 'IMPORT' | 'HTTP' = 'FILL';
  breadcrumbs: Breadcrumb[];
  formUnchanged: boolean;
  isReadOnly: boolean = false;
  groups: Group[];
  isHomepage: boolean;

  name = signal<string | undefined>(undefined);

  pages: Page[] = [];
  fetcherSchema$: Observable<GioJsonSchema | undefined> = of();

  private destroyRef = inject(DestroyRef);
  private initialFormValue: unknown;
  sourceConfigurationChanged$: Observable<boolean> = of(false);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
    private readonly groupService: GroupV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
    private readonly fetcherService: FetcherService,
  ) {}

  ngOnInit(): void {
    this.isHomepage = this.page.homepage === true;
    this.name.set(this.page.name);
    this.isReadOnly = this.api.originContext?.origin === 'KUBERNETES' || !this.permissionService.hasAnyMatching(['api-documentation-u']);

    const accessControlGroups = this.page.accessControls
      ? this.page.accessControls.filter((ac) => ac.referenceType === 'GROUP').map((ac) => ac.referenceId)
      : [];

    this.form = new FormGroup<EditPageForm>({
      pageConfiguration: new FormGroup<PageConfigurationForm>({
        name: new FormControl<string>(this.page.name ?? '', [Validators.required]),
        visibility: new FormControl<Visibility>(this.page.visibility ?? 'PUBLIC', [Validators.required]),
        excludeGroups: new FormControl<boolean>(this.page.excludedAccessControls === true),
        accessControlGroups: new FormControl<string[]>(accessControlGroups),
      }),
      content: new FormControl<string>(this.page.content ?? '', [Validators.required]),
      openApiConfiguration: new FormGroup<OpenApiConfiguration>({
        viewer: new FormControl(this.page.configuration?.['viewer'] ?? 'Swagger'),
        entrypointAsBasePath: new FormControl(this.parseConfigurationStringToBoolean(this.page.configuration?.['entrypointAsBasePath'])),
        entrypointsAsServers: new FormControl(this.parseConfigurationStringToBoolean(this.page.configuration?.['entrypointsAsServers'])),
        tryItURL: new FormControl(this.page.configuration?.['tryItURL'] ?? ''),
        tryIt: new FormControl<boolean>(this.parseConfigurationStringToBoolean(this.page.configuration?.['tryIt'])),
        tryItAnonymous: new FormControl<boolean>(this.parseConfigurationStringToBoolean(this.page.configuration?.['tryItAnonymous'])),
        showURL: new FormControl<boolean>(this.parseConfigurationStringToBoolean(this.page.configuration?.['showURL'])),
        displayOperationId: new FormControl<boolean>(
          this.parseConfigurationStringToBoolean(this.page.configuration?.['displayOperationId']),
        ),
        usePkce: new FormControl<boolean>(this.parseConfigurationStringToBoolean(this.page.configuration?.['usePkce'])),
        docExpansion: new FormControl<string>(this.page.configuration?.['docExpansion'] ?? 'none'),
        enableFiltering: new FormControl<boolean>(this.parseConfigurationStringToBoolean(this.page.configuration?.['enableFiltering'])),
        showExtensions: new FormControl<boolean>(this.parseConfigurationStringToBoolean(this.page.configuration?.['showExtensions'])),
        showCommonExtensions: new FormControl<boolean>(
          this.parseConfigurationStringToBoolean(this.page.configuration?.['showCommonExtensions']),
        ),
        maxDisplayedTags: new FormControl<number>(this.parseConfigurationStringToNumber(this.page.configuration?.['maxDisplayedTags'])),
      }),
      sourceConfiguration: new FormControl<unknown>(this.page.source?.configuration),
    });

    if (this.isReadOnly) {
      this.form.disable();
    }

    if (this.page.source?.type) {
      this.form.controls.content.disable();
    }

    this.form.controls.openApiConfiguration.controls.entrypointsAsServers.valueChanges
      .pipe(distinctUntilChanged(isEqual), takeUntilDestroyed(this.destroyRef))
      .subscribe((enabled) => {
        if (enabled) {
          this.form.controls.openApiConfiguration.controls.tryItURL.disable();
        } else {
          this.form.controls.openApiConfiguration.controls.tryItURL.enable();
        }
      });

    this.initialFormValue = this.form.getRawValue();

    this.form.valueChanges
      .pipe(
        distinctUntilChanged(isEqual),
        tap((_) => {
          this.formUnchanged = isEqual(this.initialFormValue, this.form.getRawValue());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    combineLatest([this.apiDocumentationService.getApiPages(this.api.id, this.page.parentId ?? 'ROOT'), this.groupService.list(1, 999)])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([pagesResponse, groupsResponse]) => {
          this.breadcrumbs = pagesResponse.breadcrumb;
          this.pages = pagesResponse.pages;
          this.groups = groupsResponse?.data ?? [];
        },
      });

    this.fetcherSchema$ = this.fetcherService.getList().pipe(
      map((fetchers) => {
        const currentSchema = fetchers.find((f) => f.id === this.page.source.type)?.schema;
        return currentSchema ? JSON.parse(currentSchema) : undefined;
      }),
      catchError((_) => of([])),
    );

    this.sourceConfigurationChanged$ = this.form.controls.sourceConfiguration.valueChanges.pipe(
      map((_) => !isEqual(this.initialFormValue['sourceConfiguration'], this.form.getRawValue().sourceConfiguration)),
    );
  }

  onGoBackRouterLink(): void {
    this.router.navigate(this.goBackRouterLink, {
      relativeTo: this.activatedRoute,
      queryParams: { parentId: this.page.parentId ?? 'ROOT' },
    });
  }

  update() {
    this.updatePage()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBarService.success('Page updated successfully');
          this.goBackToPageList();
        },
      });
  }

  updateAndPublish() {
    this.updatePage()
      .pipe(
        switchMap((page) => this.apiDocumentationService.publishDocumentationPage(this.api.id, page.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Page updated and published successfully');

          this.goBackToPageList();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Cannot publish page');
        },
      });
  }

  private updatePage(): Observable<Page> {
    const formValue = this.form.getRawValue();
    return this.apiDocumentationService.getApiPage(this.api.id, this.page.id).pipe(
      switchMap((page) => {
        const nonGroupAccessControls = page.accessControls ? page.accessControls.filter((ac) => ac.referenceType !== 'GROUP') : [];
        const selectedGroupAccessControls: AccessControl[] = formValue.pageConfiguration.accessControlGroups.map((referenceId) => ({
          referenceId,
          referenceType: 'GROUP',
        }));

        const updateDocumentation: EditDocumentation = {
          ...page,
          name: formValue.pageConfiguration.name,
          visibility: formValue.pageConfiguration.visibility,
          content: formValue.content,
          excludedAccessControls: formValue.pageConfiguration.excludeGroups,
          accessControls: [...nonGroupAccessControls, ...selectedGroupAccessControls],
          ...(this.page.type === 'SWAGGER' ? { configuration: { ...formValue.openApiConfiguration } } : {}),
          ...(this.page.source?.type
            ? {
                source: {
                  type: this.page.source.type,
                  configuration: formValue.sourceConfiguration,
                },
              }
            : {}),
        };
        return this.apiDocumentationService.updateDocumentationPage(this.api.id, this.page.id, updateDocumentation);
      }),
      catchError((err) => {
        this.snackBarService.error(err?.error?.message ?? 'Cannot update page');
        return EMPTY;
      }),
    );
  }

  goBackToPageList() {
    this.router.navigate(['../../'], {
      relativeTo: this.activatedRoute,
      queryParams: { parentId: this.page.parentId ?? 'ROOT' },
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
        switchMap((_) => this.apiDocumentationService.deleteDocumentationPage(this.api.id, this.page.id)),
        takeUntilDestroyed(this.destroyRef),
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

  refreshContent() {
    this.apiDocumentationService
      .fetchDocumentationPage(this.api.id, this.page.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.initialFormValue['content'] = page.content;

          this.form.controls.content.setValue(page.content);
          this.form.controls.content.updateValueAndValidity();
          this.snackBarService.success('Page content refreshed successfully.');
        },
        error: (_) => {
          this.snackBarService.error('Error while refreshing page content.');
        },
      });
  }

  onGioSchemaFormReady(ready: boolean) {
    if (ready) {
      this.initialFormValue['sourceConfiguration'] = this.form.getRawValue().sourceConfiguration;
    }
  }

  private parseConfigurationStringToBoolean(configString: string): boolean {
    return configString ? JSON.parse(configString) === true : false;
  }

  private parseConfigurationStringToNumber(configString: string): number {
    return configString ? Number(configString) : -1;
  }
}
