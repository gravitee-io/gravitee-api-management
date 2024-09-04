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
  GioFormJsonSchemaModule,
  GioFormSelectionInlineModule,
  GioFormSlideToggleModule,
  GioJsonSchema,
} from '@gravitee/ui-particles-angular';
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
import { catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';
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
  Group,
  Page,
  PageSource,
  PageType,
  Visibility,
} from '../../../../../entities/management-api-v2';
import { GioPermissionModule } from '../../../../../shared/components/gio-permission/gio-permission.module';
import { ApiDocumentationV4ContentEditorComponent } from '../api-documentation-v4-content-editor/api-documentation-v4-content-editor.component';
import { ApiDocumentationV4FileUploadComponent } from '../api-documentation-v4-file-upload/api-documentation-v4-file-upload.component';
import {
  ApiDocumentationV4PageConfigurationComponent,
  PageConfigurationForm,
} from '../api-documentation-v4-page-configuration/api-documentation-v4-page-configuration.component';
import { ApiDocumentationV4PageHeaderComponent } from '../api-documentation-v4-page-header/api-documentation-v4-page-header.component';

interface CreatePageForm {
  stepOne: FormGroup<PageConfigurationForm>;
  content: FormControl<string>;
  sourceType: FormControl<string>;
  source: FormControl<string>;
}

interface FetcherVM {
  id: string;
  name: string;
  schema: GioJsonSchema;
  iconPath: string;
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
    ApiDocumentationV4ContentEditorComponent,
    ApiDocumentationV4FileUploadComponent,
    ApiDocumentationV4PageConfigurationComponent,
    ApiDocumentationV4PageHeaderComponent,
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
  sourceConfiguration?: FormControl<undefined | unknown>;

  defaultSourceType: string = 'FILL';
  breadcrumbs: Breadcrumb[];
  page: Page;
  isReadOnly: boolean = false;
  groups: Group[];

  name = signal<string | undefined>(undefined);

  pages: Page[] = [];

  fetchers: FetcherVM[];
  selectedFetcherSchema: GioJsonSchema;
  private destroyRef = inject(DestroyRef);

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
    if (this.createHomepage) {
      this.name.set('Homepage');
    }

    this.form = new FormGroup<CreatePageForm>({
      stepOne: new FormGroup<PageConfigurationForm>({
        name: new FormControl<string>(this.name(), [Validators.required]),
        visibility: new FormControl<Visibility>('PUBLIC', [Validators.required]),
        accessControlGroups: new FormControl<string[]>([]),
        excludeGroups: new FormControl<boolean>(false),
      }),
      content: new FormControl<string>('', [Validators.required]),
      sourceType: new FormControl<string>(this.defaultSourceType, [Validators.required]),
      source: new FormControl<string>(''),
    });

    this.sourceConfiguration = new FormControl<undefined | unknown>({});

    this.isReadOnly = this.api.originContext?.origin === 'KUBERNETES';

    if (this.isReadOnly) {
      this.form.disable({ emitEvent: false });
    }

    combineLatest([this.apiDocumentationService.getApiPages(this.api.id, this.parentId ?? 'ROOT'), this.groupService.list(1, 999)])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([pagesResponse, groupsResponse]) => {
          this.breadcrumbs = pagesResponse.breadcrumb;
          this.pages = pagesResponse.pages;

          this.groups = groupsResponse?.data ?? [];
        },
      });

    this.fetcherService
      .getList()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(
        (fetchers) =>
          (this.fetchers = fetchers.map((f) => {
            return {
              id: f.id,
              name: f.name,
              schema: JSON.parse(f.schema),
              iconPath: `assets/logo_${f.name.toLowerCase()}-fetcher.svg`,
            };
          })),
      );

    this.form.controls.sourceType.valueChanges.pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
      if (value === 'EXTERNAL') {
        this.form.controls.content.clearValidators();
        this.form.controls.source.addValidators([Validators.required]);
        this.sourceConfiguration?.addValidators([Validators.required]);
      } else {
        this.form.controls.content.addValidators([Validators.required]);
        this.form.controls.source.clearValidators();
        this.sourceConfiguration?.clearValidators();
      }
      this.form.controls.content.updateValueAndValidity();
      this.form.controls.source.updateValueAndValidity();
      this.sourceConfiguration?.updateValueAndValidity();
    });

    this.form.controls.source.valueChanges
      .pipe(
        distinctUntilChanged(),
        tap((_) => {
          // Clear variable, wait, and re-init is a hack to force re-rendering of GioJsonSchemaForm
          this.selectedFetcherSchema = undefined;
          this.sourceConfiguration = undefined;
        }),
        debounceTime(10),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((value) => {
        this.sourceConfiguration = new FormControl<unknown>({}, [Validators.required]);
        this.selectedFetcherSchema = this.fetchers.find((f) => f.id === value)?.schema;
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

  goBackToPageList() {
    this.router.navigate(['../../'], {
      relativeTo: this.activatedRoute,
      queryParams: { parentId: this.parentId ?? 'ROOT' },
    });
  }

  private obtainSource(source: string, configuration: unknown | undefined): PageSource {
    return {
      type: source,
      configuration,
    };
  }

  private createPage(): Observable<Page> {
    const formValue = this.form.getRawValue();
    // Only Markdown, Swagger, and AsyncAPI pages can be created
    if (this.pageType !== 'MARKDOWN' && this.pageType !== 'SWAGGER' && this.pageType !== 'ASYNCAPI') {
      this.snackBarService.error(`Cannot create page with type [${this.pageType}]`);
      return EMPTY;
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
      ...(formValue.sourceType === 'EXTERNAL' &&
        formValue.source && {
          source: this.obtainSource(formValue.source, this.sourceConfiguration.value),
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
