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
import { Component, computed, DestroyRef, HostListener, inject, OnInit, Signal, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { LowerCasePipe } from '@angular/common';
import { DomSanitizer } from '@angular/platform-browser';
import { GioBannerModule, GioFormFilePickerModule, GioFormSelectionInlineModule, NewFile } from '@gravitee/ui-particles-angular';
import { isEqual, pick } from 'lodash';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EMPTY, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {
  PortalNavigationApi,
  PortalNavigationFolder,
  PortalNavigationItem,
  PortalNavigationItemSource,
  PortalNavigationItemType,
  PortalNavigationLink,
  PortalNavigationPage,
  PortalPageContentType,
  PortalVisibility,
} from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { urlValidator } from '../../../shared/validators/url.validator';
import { getPublicVisibilityDisabledTooltip, isPublicVisibilityDisabled } from '../visibility-toggle.util';
import { NavigationItemSourceEditorComponent } from '../navigation-item-source-editor/navigation-item-source-editor.component';
import {
  extractTitleFromFileName,
  ImportedFileContent,
  ImportFileError,
  IMPORTABLE_FILE_EXTENSIONS,
  readImportedFile,
  validateImportFile,
} from '../portal-page-content-import.util';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

export type SectionEditorDialogMode = 'create' | 'edit';

export type SectionEditorDialogItemType = Exclude<PortalNavigationItemType, 'API' | 'API_PRODUCT' | 'SUBSCRIPTION_FORM'>;

interface SectionEditorDialogCreateData {
  mode: 'create';
  type: SectionEditorDialogItemType;
  parentItem?: PortalNavigationItem;
}

interface SectionEditorDialogEditData {
  mode: 'edit';
  type: PortalNavigationItemType;
  existingItem: PortalNavigationItem;
  parentItem?: PortalNavigationItem;
}

export type SectionEditorDialogData = SectionEditorDialogCreateData | SectionEditorDialogEditData;

export interface SectionEditorDialogResult {
  title: string;
  visibility: PortalVisibility;
  url?: string;
  contentType?: PortalPageContentType;
  source?: PortalNavigationItemSource;
  content?: string;
}

export type SectionContentSource = 'FILL' | 'IMPORT_FILE' | 'EXTERNAL';

export interface PortalPageTypeOption {
  value: PortalPageContentType;
  label: string;
  icon: string;
  available: boolean;
}

export const PORTAL_PAGE_CONTENT_TYPE_OPTIONS: PortalPageTypeOption[] = [
  { value: 'GRAVITEE_MARKDOWN', label: 'Markdown', icon: 'gio:gravitee', available: true },
  { value: 'OPENAPI', label: 'OpenAPI', icon: 'gio:open-api', available: true },
  { value: 'ASYNCAPI', label: 'AsyncAPI', icon: 'gio:async-api', available: true },
];

const TITLE_FIELD_LABEL_BY_TYPE: Record<PortalNavigationItemType, string> = {
  API: 'API Display Name',
  API_PRODUCT: 'API Product Display Name',
  FOLDER: 'Folder Title',
  LINK: 'Link Title',
  PAGE: 'Page Title',
  // SUBSCRIPTION_FORM items are never created or edited through this dialog (edited via the
  // dedicated subscription-form screen instead); present for Record exhaustiveness only.
  SUBSCRIPTION_FORM: 'Subscription Form Title',
};

interface SectionFormControls {
  title: FormControl<string>;
  isPrivate: FormControl<boolean>;
  url?: FormControl<string>; // Optional for 'LINK' type
  contentType?: FormControl<PortalPageContentType>; // Optional for 'PAGE' create only
}

interface SectionFormValues {
  title: string;
  isPrivate: boolean;
  url?: string;
  contentType?: PortalPageContentType;
}

type SectionForm = FormGroup<SectionFormControls>;

@Component({
  selector: 'section-editor-dialog',
  imports: [
    MatDialogModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSlideToggleModule,
    MatInputModule,
    GioBannerModule,
    GioFormSelectionInlineModule,
    LowerCasePipe,
    MatTooltipModule,
    NavigationItemSourceEditorComponent,
    GioFormFilePickerModule,
    MatIconModule,
  ],
  templateUrl: './section-editor-dialog.component.html',
  styleUrls: ['./section-editor-dialog.component.scss'],
})
export class SectionEditorDialogComponent implements OnInit {
  form: SectionForm = new FormGroup<SectionFormControls>({
    title: new FormControl<string>('', { validators: [Validators.required], nonNullable: true }),
    isPrivate: new FormControl(false),
  });
  public initialFormValues: SectionFormValues;

  public type: PortalNavigationItemType;
  public mode: SectionEditorDialogMode;
  public title: string;
  public titleFieldLabel: string;
  public titleLockedBySource = false;
  readonly pageContentTypeOptions = PORTAL_PAGE_CONTENT_TYPE_OPTIONS;

  // --- External source state ---
  readonly contentSourceControl = new FormControl<SectionContentSource>('FILL', { nonNullable: true });
  readonly contentSource = toSignal(this.contentSourceControl.valueChanges, { initialValue: this.contentSourceControl.value });
  readonly useExternalSourceControl = new FormControl<boolean>(false, { nonNullable: true });
  readonly useExternalSource = toSignal(this.useExternalSourceControl.valueChanges, { initialValue: this.useExternalSourceControl.value });
  readonly isSourceChoiceStep = signal(false);
  public initialSource: PortalNavigationItemSource | undefined;
  private readonly sourceEditor = viewChild(NavigationItemSourceEditorComponent);

  // --- File import state ---
  readonly importFileControl = new FormControl<(NewFile | string)[]>([]);
  readonly importFileAccept = IMPORTABLE_FILE_EXTENSIONS.join(',');
  readonly importedFile = signal<ImportedFileContent | null>(null);
  readonly importedFileName = signal<string | null>(null);
  readonly importedContentTypeLabel = computed(() => {
    const contentType = this.importedFile()?.contentType;
    return PORTAL_PAGE_CONTENT_TYPE_OPTIONS.find(option => option.value === contentType)?.label ?? '';
  });

  showFileImport(): boolean {
    return this.isCreatePageFlow() && this.contentSource() === 'IMPORT_FILE';
  }

  showPageTypeSelection(): boolean {
    return this.mode === 'create' && this.type === 'PAGE' && this.contentSource() !== 'IMPORT_FILE';
  }

  isCreatePageFlow(): boolean {
    return this.mode === 'create' && this.type === 'PAGE';
  }

  canConfigureSourceOnEdit(): boolean {
    if (this.mode !== 'edit') {
      return false;
    }
    // A folder only gets a source through the import (the backend rejects attaching one by update):
    // offer the source section solely on import-managed folders, to edit or remove their source
    if (this.type === 'FOLDER') {
      return !!this.initialSource;
    }
    return this.type === 'PAGE';
  }

  isExternalSourceActive(): boolean {
    if (this.isCreatePageFlow()) {
      return this.contentSource() === 'EXTERNAL';
    }
    return this.canConfigureSourceOnEdit() && this.useExternalSource();
  }

  private readonly dialogRef = inject(MatDialogRef<SectionEditorDialogComponent, SectionEditorDialogResult>);
  private readonly data: SectionEditorDialogData = inject(MAT_DIALOG_DATA);
  private readonly iconRegistry = inject(MatIconRegistry);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly apiService = inject(ApiV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);
  readonly publicDisabled: Signal<boolean> = computed(() => {
    return isPublicVisibilityDisabled(this.data.parentItem);
  });
  readonly publicDisabledTooltip: Signal<string> = computed(() => {
    return getPublicVisibilityDisabledTooltip(this.data.parentItem);
  });
  readonly linkedApiName: Signal<string | null> = toSignal(this.loadLinkedApiName(), { initialValue: null });
  public buttonTitle: string;

  constructor() {
    this.iconRegistry.addSvgIconInNamespace('gio', 'async-api', this.sanitizer.bypassSecurityTrustResourceUrl('assets/logo_asyncapi.svg'));
    this.type = this.data.type;
    this.mode = this.data.mode;
    this.titleFieldLabel = TITLE_FIELD_LABEL_BY_TYPE[this.type];
    if (this.data.mode === 'create') {
      this.title = `Add ${this.type.toLowerCase()}`;
      this.buttonTitle = 'Add';
    } else {
      this.title = `Edit "${this.data.existingItem.title}" ${this.type.toLowerCase()}`;
      this.buttonTitle = 'Save';
    }
  }

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent) {
    if (!this.formIsUnchanged()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  ngOnInit(): void {
    this.addTypeSpecificControls();
    this.prefillExistingItem();
    this.syncVisibilityControlState();

    if (this.isCreatePageFlow()) {
      this.isSourceChoiceStep.set(true);
    }

    this.importFileControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(files => this.onFilePicked(files ?? []));

    this.useExternalSourceControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(useSource => {
      if (!this.initialSource) {
        return;
      }
      this.titleLockedBySource = useSource;
      if (useSource) {
        this.form.controls.title.disable({ emitEvent: false });
      } else {
        this.form.controls.title.enable({ emitEvent: false });
      }
    });

    this.initialFormValues = this.form.getRawValue();
  }

  continueToDetails(): void {
    this.isSourceChoiceStep.set(false);
  }

  private onFilePicked(files: (NewFile | string)[]): void {
    this.importedFile.set(null);
    this.importedFileName.set(null);

    const picked = files.find(file => file instanceof NewFile);
    if (!picked?.file) {
      return;
    }
    const file = picked.file;

    const validationError = validateImportFile(file);
    if (validationError) {
      this.snackBarService.error(validationError);
      return;
    }

    readImportedFile(file)
      .pipe(
        catchError(error => {
          this.snackBarService.error(error instanceof ImportFileError ? error.message : `Failed to read "${file.name}"`);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(imported => {
        this.importedFile.set(imported);
        this.importedFileName.set(file.name);
        this.form.controls.contentType?.setValue(imported.contentType);
        if (!this.form.controls.title.value.trim()) {
          this.form.controls.title.setValue(extractTitleFromFileName(file.name));
        }
      });
  }

  backToSourceChoice(): void {
    this.isSourceChoiceStep.set(true);
  }

  private syncVisibilityControlState(): void {
    const isPrivateControl = this.form.controls.isPrivate;

    if (this.publicDisabled()) {
      isPrivateControl.setValue(true, { emitEvent: false });
      isPrivateControl.disable({ emitEvent: false });
      return;
    }

    isPrivateControl.enable({ emitEvent: false });
  }

  private addTypeSpecificControls(): void {
    if (this.type === 'LINK') {
      this.form.addControl(
        'url',
        new FormControl<string>('', {
          validators: [Validators.required, urlValidator()],
          nonNullable: true,
        }),
      );
    }
    if (this.isCreatePageFlow()) {
      this.form.addControl('contentType', new FormControl<PortalPageContentType>('GRAVITEE_MARKDOWN', { nonNullable: true }));
    }
  }

  private prefillExistingItem(): void {
    if (this.data.mode === 'edit') {
      this.form.patchValue({
        ...(this.data.existingItem.type === 'LINK' ? { url: (this.data.existingItem as PortalNavigationLink).url } : {}),
        title: this.data.existingItem.title,
        isPrivate: this.data.existingItem.visibility === 'PRIVATE',
      });
      const existingItem = this.data.existingItem;
      if (existingItem.type === 'PAGE' || existingItem.type === 'FOLDER') {
        this.initialSource = (existingItem as PortalNavigationPage | PortalNavigationFolder).source;
      }
      if (this.initialSource) {
        this.useExternalSourceControl.setValue(true);
        this.titleLockedBySource = true;
        this.form.controls.title.disable({ emitEvent: false });
      }
    }
  }

  onSubmit(): void {
    if (this.isSubmitDisabled()) {
      return;
    }
    const formValues = this.form.getRawValue();

    this.dialogRef.close({
      title: formValues.title,
      visibility: formValues.isPrivate ? 'PRIVATE' : 'PUBLIC',
      ...(this.type === 'LINK' ? { url: formValues.url! } : {}),
      ...(this.isCreatePageFlow() && formValues.contentType ? { contentType: formValues.contentType } : {}),
      ...(this.isExternalSourceActive() ? { source: this.sourceEditor()?.buildSource() ?? undefined } : {}),
      ...(this.showFileImport() && this.importedFile() ? { content: this.importedFile()!.content } : {}),
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  isSubmitDisabled(): boolean {
    if (!this.form.valid) {
      return true;
    }
    if (this.showFileImport()) {
      return !this.importedFile();
    }
    if (this.isExternalSourceActive()) {
      const editor = this.sourceEditor();
      if (!editor || editor.saveDisabled()) {
        return true;
      }
      const comparableInitialSource = this.initialSource
        ? pick(this.initialSource, ['type', 'configuration', 'useAutoFetch', 'fetchCron'])
        : null;
      return this.formIsUnchanged() && isEqual(editor.buildSource(), comparableInitialSource);
    }
    const sourceRemoved = !!this.initialSource && this.canConfigureSourceOnEdit() && !this.useExternalSource();
    return this.formIsUnchanged() && !sourceRemoved;
  }

  formIsUnchanged(): boolean {
    return isEqual(this.form.getRawValue(), this.initialFormValues);
  }

  private loadLinkedApiName(): Observable<string | null> {
    if (this.data.mode !== 'edit' || this.data.type !== 'API') {
      return of(null);
    }

    const apiId = (this.data.existingItem as PortalNavigationApi).apiId;
    return this.apiService.resolveNameById(apiId);
  }
}
