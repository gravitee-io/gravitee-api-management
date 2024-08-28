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
import { Input, Component, OnInit, signal, computed, inject, DestroyRef } from '@angular/core';
import { AsyncPipe, NgOptimizedImage } from '@angular/common';
import { GioFormJsonSchemaModule, GioFormSelectionInlineModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatStepperModule } from '@angular/material/stepper';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { combineLatest, EMPTY, Observable } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { catchError, map, switchMap } from 'rxjs/operators';
import { MatTooltip } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ApiDocumentationV2Service } from '../../../../../services-ngx/api-documentation-v2.service';
import { GroupV2Service } from '../../../../../services-ngx/group-v2.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { FetcherService } from '../../../../../services-ngx/fetcher.service';
import { ApiDocumentationV4VisibilityComponent } from '../api-documentation-v4-visibility/api-documentation-v4-visibility.component';
import {
  Api,
  Breadcrumb,
  CreateDocumentation,
  CreateDocumentationType,
  getLogoForPageType,
  getTooltipForPageType,
  Group,
  Page,
  PageSource,
  PageType,
} from '../../../../../entities/management-api-v2';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { ApiDocumentationV4BreadcrumbComponent } from '../api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.component';
import { ApiDocumentationV4ContentEditorComponent } from '../api-documentation-v4-content-editor/api-documentation-v4-content-editor.component';
import { ApiDocumentationV4FileUploadComponent } from '../api-documentation-v4-file-upload/api-documentation-v4-file-upload.component';
import {
  ApiDocumentationV4PageConfigurationComponent,
  INIT_PAGE_CONFIGURATION_FORM,
  PageConfigurationData,
  PageConfigurationForm,
} from '../api-documentation-v4-page-configuration/api-documentation-v4-page-configuration.component';

interface CreatePageForm {
  stepOne: FormGroup<PageConfigurationForm>;
  content: FormControl<string>;
  source: FormControl<string>;
  sourceConfiguration: FormControl<undefined | unknown>;
}

@Component({
  selector: 'documentation-new-page',
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
    MatStepperModule,
    NgOptimizedImage,
    ReactiveFormsModule,
    MatTooltip,
    ApiDocumentationV4VisibilityComponent,
    ApiDocumentationV4BreadcrumbComponent,
    ApiDocumentationV4ContentEditorComponent,
    ApiDocumentationV4FileUploadComponent,
    ApiDocumentationV4PageConfigurationComponent,
  ],
  templateUrl: './documentation-new-page.component.html',
  styleUrl: './documentation-new-page.component.scss',
})
export class DocumentationNewPageComponent implements OnInit {
  @Input()
  goBackRouterLink: string[];

  @Input()
  createHomepage: boolean;

  @Input()
  api: Api;

  @Input()
  pageType: PageType;

  @Input()
  parentId?: string;

  form: FormGroup<CreatePageForm>;
  source: 'FILL' | 'IMPORT' | 'HTTP' = 'FILL';
  breadcrumbs: Breadcrumb[];
  page: Page;
  iconUrl: string;
  iconTooltip: string;
  isReadOnly: boolean = false;
  groups: Group[];
  stepOneData: PageConfigurationData = undefined;

  name = signal<string | undefined>(undefined);
  headerPageName = computed(() => this.name() ?? 'Add new page');

  pages: Page[] = [];

  schema$: Observable<any>;
  private destroyRef = inject(DestroyRef);
  private readonly httpFetcherName = 'http-fetcher';

  private readonly httpValue = 'HTTP';

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
    this.form = new FormGroup<CreatePageForm>({
      stepOne: INIT_PAGE_CONFIGURATION_FORM(),
      content: new FormControl<string>('', [Validators.required]),
      source: new FormControl<string>(this.source, [Validators.required]),
      sourceConfiguration: new FormControl<undefined | unknown>({}),
    });

    if (this.createHomepage) {
      this.name.set('Homepage');
    }

    this.isReadOnly = this.api.originContext?.origin === 'KUBERNETES';

    if (this.isReadOnly) {
      this.form.disable({ emitEvent: false });
    }

    combineLatest([this.apiDocumentationService.getApiPages(this.api.id, this.parentId ?? 'ROOT'), this.groupService.list(1, 999)])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([pagesResponse, groupsResponse]) => {
          this.iconUrl = getLogoForPageType(this.pageType);
          this.iconTooltip = getTooltipForPageType(this.pageType);

          this.breadcrumbs = pagesResponse.breadcrumb;
          this.pages = pagesResponse.pages;

          this.groups = groupsResponse?.data ?? [];
        },
      });

    this.schema$ = this.fetcherService.getList().pipe(
      map((list) => list.find((fetcher) => fetcher.id === this.httpFetcherName)?.schema),
      map((schema) => JSON.parse(schema)),
    );

    this.form.controls.source.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
      if (value === this.httpValue) {
        this.form.controls.content.clearValidators();
      } else {
        this.form.controls.content.addValidators([Validators.required]);
      }
      this.form.controls.content.updateValueAndValidity();
    });
  }

  onGoBackRouterLink(): void {
    this.router.navigate(this.goBackRouterLink, { relativeTo: this.activatedRoute });
  }

  create() {
    this.createPage()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.snackBarService.success('Page created successfully');
        this.goBackToPageList();
      });
  }

  createAndPublish() {
    this.createPage()
      .pipe(
        switchMap((page) => this.apiDocumentationService.publishDocumentationPage(this.api.id, page.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBarService.success('Page created and published successfully');
          this.goBackToPageList();
        },
        error: (error) => {
          this.snackBarService.error(error?.error?.message ?? 'Cannot publish page');
        },
      });
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
    this.router.navigate(['../../'], {
      relativeTo: this.activatedRoute,
      queryParams: { parentId: this.parentId ?? 'ROOT' },
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
      homepage: this.createHomepage === true,
      content: formValue.content,
      parentId: this.parentId || 'ROOT',
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
}
