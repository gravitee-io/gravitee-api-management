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
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormJsonSchemaModule,
  GioFormSelectionInlineModule,
  GioFormSlideToggleModule,
} from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { combineLatest, EMPTY, Observable } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { catchError, filter, switchMap, tap } from 'rxjs/operators';
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
import { AccessControl, Api, Breadcrumb, EditDocumentation, Group, Page } from '../../../../../entities/management-api-v2';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { ApiDocumentationV4ContentEditorComponent } from '../api-documentation-v4-content-editor/api-documentation-v4-content-editor.component';
import { ApiDocumentationV4FileUploadComponent } from '../api-documentation-v4-file-upload/api-documentation-v4-file-upload.component';
import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import {
  ApiDocumentationV4PageConfigurationComponent,
  INIT_PAGE_CONFIGURATION_FORM,
  PageConfigurationData,
  PageConfigurationForm,
} from '../api-documentation-v4-page-configuration/api-documentation-v4-page-configuration.component';
import { ApiDocumentationV4PageHeaderComponent } from '../api-documentation-v4-page-header/api-documentation-v4-page-header.component';

interface EditPageForm {
  pageConfiguration: FormGroup<PageConfigurationForm>;
  content: FormControl<string>;
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

  form: FormGroup<EditPageForm> = new FormGroup<EditPageForm>({
    pageConfiguration: INIT_PAGE_CONFIGURATION_FORM(),
    content: new FormControl<string>('', [Validators.required]),
  });
  source: 'FILL' | 'IMPORT' | 'HTTP' = 'FILL';
  breadcrumbs: Breadcrumb[];
  formUnchanged: boolean;
  isReadOnly: boolean = false;
  groups: Group[];
  pageConfigurationData: PageConfigurationData = undefined;
  isHomepage: boolean;

  name = signal<string | undefined>(undefined);

  pages: Page[] = [];

  private initialAccessControlGroups: string[] = [];
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    private readonly apiDocumentationService: ApiDocumentationV2Service,
    private readonly groupService: GroupV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isHomepage = this.page.homepage === true;
    this.name.set(this.page.name);
    this.isReadOnly = this.api.originContext?.origin === 'KUBERNETES' || !this.permissionService.hasAnyMatching(['api-documentation-u']);

    if (this.isReadOnly) {
      this.form.disable();
    }

    this.initialAccessControlGroups = this.page.accessControls
      ? this.page.accessControls.filter((ac) => ac.referenceType === 'GROUP').map((ac) => ac.referenceId)
      : [];


    this.form.controls.content.setValue(this.page.content);

    this.form.valueChanges
      .pipe(
        tap((value) => {
          if (this.page) {
            this.formUnchanged =
              this.page.name === value.pageConfiguration?.name &&
              this.page.visibility === value.pageConfiguration?.visibility &&
              this.page.content === value.content &&
              (this.page.excludedAccessControls === undefined ||
                this.page.excludedAccessControls === value.pageConfiguration.excludeGroups ||
                (value.pageConfiguration.excludeGroups === undefined && value.pageConfiguration.visibility === 'PUBLIC')) &&
              isEqual(this.initialAccessControlGroups, value.pageConfiguration.accessControlGroups);
          }
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
}
