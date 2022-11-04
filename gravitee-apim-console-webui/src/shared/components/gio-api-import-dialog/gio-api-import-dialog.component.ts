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
import { Component, Inject, OnDestroy } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { NewFile } from '@gravitee/ui-particles-angular';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { Api } from '../../../entities/api';
import { PolicyListItem } from '../../../entities/policy';
import { ApiService } from '../../../services-ngx/api.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

const allowedFileExtensions = ['yml', 'yaml', 'json', 'wsdl', 'xml'] as const;

type FileExtension = typeof allowedFileExtensions[number];

export type GioApiImportDialogData = {
  policies?: PolicyListItem[];
  apiId?: string;
  // Default to 2.0.0
  definitionVersion?: string;
};

@Component({
  selector: 'gio-api-import-dialog',
  template: require('./gio-api-import-dialog.component.html'),
  styles: [require('./gio-api-import-dialog.component.scss')],
})
export class GioApiImportDialogComponent implements OnDestroy {
  tabLabels = {
    UploadFile: 'Upload a file',
    SwaggerOpenAPI: 'Swagger / OpenAPI',
    ApiDefinition: 'API definition',
    WSDL: 'WSDL',
  };

  importType: 'WSDL' | 'SWAGGER' | 'GRAVITEE';

  displayImportConfig = false;

  policies = [];
  isUpdateMode = false;
  updateModeApiId?: string;

  filePickerValue = [];
  importFile: File;
  importFileContent: string;

  descriptorUrlForm = new FormControl();

  configsForm: FormGroup;
  importPolicyPathsIntermediate = false;

  public get isImportValid(): boolean {
    return !!this.importType && (!!this.importFile || !!this.descriptorUrlForm?.value);
  }

  private unsubscribe$: Subject<void> = new Subject<void>();

  private definitionVersion: string;

  constructor(
    private readonly dialogRef: MatDialogRef<GioApiImportDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: GioApiImportDialogData,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.policies = dialogData?.policies ?? [];
    this.isUpdateMode = !!dialogData?.apiId ?? false;
    this.updateModeApiId = dialogData?.apiId ?? undefined;
    this.definitionVersion = dialogData?.definitionVersion ?? '2.0.0';

    this.configsForm = new FormGroup({
      importDocumentation: new FormControl(false),
      importPathMapping: new FormControl(false),
      importPolicyPaths: new FormControl(false),
      importPolicies: new FormGroup(
        this.policies.reduce((acc, policy) => {
          acc[policy.id] = new FormControl(false);
          return acc;
        }, {}),
      ),
    });

    // select all policy when importPolicyPaths is checked
    this.configsForm
      .get('importPolicyPaths')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        this.importPolicyPathsIntermediate = false;
        this.configsForm.get('importPolicies').patchValue(
          this.policies.reduce((acc, policy) => {
            acc[policy.id] = value;
            return acc;
          }, {}),
          { emitEvent: false },
        );
      });

    // handle intermediate state for importPolicyPaths and check importPolicyPaths if at least one policy is checked
    this.configsForm
      .get('importPolicies')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((importPoliciesValue) => {
        const nbPoliciesChecked = Object.keys(importPoliciesValue).filter((policyId) => importPoliciesValue[policyId]).length;

        const isEmptyOrAllChecked = nbPoliciesChecked === 0 || nbPoliciesChecked === this.policies.length;
        if (isEmptyOrAllChecked) {
          this.configsForm.get('importPolicyPaths').patchValue(nbPoliciesChecked !== 0, { emitEvent: false });
          this.importPolicyPathsIntermediate = false;
          return;
        }

        this.configsForm.get('importPolicyPaths').patchValue(true, { emitEvent: false });
        this.importPolicyPathsIntermediate = true;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  async onImportFile(event: (NewFile | string)[] | undefined) {
    if (!event || event.length !== 1) {
      this.resetImportFile();
      return;
    }
    const file = event[0];

    if (!(file instanceof NewFile)) {
      return;
    }

    const extension = file.name.split('.').pop().toLowerCase() as FileExtension;
    if (!allowedFileExtensions.includes(extension)) {
      this.resetImportFile('Invalid file format. Supported file formats: ' + allowedFileExtensions.join(', '));
      return;
    }
    const fileContent = await getFileContent(file.file);

    // Find import type with extension and file content for json format
    switch (extension) {
      case 'wsdl':
      case 'xml':
        this.importType = 'WSDL';
        break;

      case 'json':
        try {
          const json = JSON.parse(fileContent);

          // Check if it's a swagger. if not consider is gravitee api definition
          this.importType = isSwaggerJsonContent(json) ? 'SWAGGER' : 'GRAVITEE';
        } catch (error) {
          this.resetImportFile('Invalid JSON file.');
        }
        break;

      case 'yml':
      case 'yaml':
        this.importType = 'SWAGGER';
        break;

      default:
        this.resetImportFile('Invalid file format.');
        return;
    }

    if (this.importType) {
      this.importFile = file.file;
      this.importFileContent = fileContent;
    }
  }

  onSelectedTab(event: MatTabChangeEvent) {
    this.importType = undefined;
    const initUrlDescriptor = () => {
      if (!this.descriptorUrlForm) {
        this.descriptorUrlForm = new FormControl('', [Validators.required]);
      }
    };

    switch (event.tab.textLabel) {
      case this.tabLabels.UploadFile:
        this.resetImportFile();
        this.descriptorUrlForm = new FormControl();
        break;

      case this.tabLabels.SwaggerOpenAPI:
        this.importType = 'SWAGGER';
        initUrlDescriptor();
        break;

      case this.tabLabels.ApiDefinition:
        this.importType = 'GRAVITEE';
        initUrlDescriptor();
        break;

      case this.tabLabels.WSDL:
        this.importType = 'WSDL';
        initUrlDescriptor();
        break;
    }
  }

  hasSomeConfigPolicies() {
    const importPolicies = this.configsForm.value.importPolicies;
    const importPolicyPaths = this.configsForm.value.importPolicyPaths;
    const nbPoliciesChecked = Object.keys(importPolicies).filter((policyId) => importPolicies[policyId]).length;
    return nbPoliciesChecked < this.policies.length && importPolicyPaths;
  }

  async onImport() {
    const configsFormValue = this.configsForm.value;

    const isFile = !!this.importFile;
    const payload = isFile ? this.importFileContent : this.descriptorUrlForm.value;

    if (!this.importFile && !this.descriptorUrlForm.value) {
      // Crazy guard. should not happen
      throw new Error('No file or url provided');
    }

    let importRequest$: Observable<Api>;

    switch (this.importType) {
      case 'WSDL':
        importRequest$ = this.apiService.importSwaggerApi(
          {
            payload: payload,
            type: isFile ? 'INLINE' : 'URL',
            format: 'WSDL',
            with_documentation: configsFormValue.importDocumentation,
            with_path_mapping: configsFormValue.importPathMapping,
            with_policies: this.policies.filter((policy) => configsFormValue.importPolicies[policy.id]).map((policy) => policy.id),
            with_policy_paths: configsFormValue.importPolicyPaths,
          },
          this.definitionVersion,
          this.updateModeApiId,
        );
        break;

      case 'SWAGGER':
        importRequest$ = this.apiService.importSwaggerApi(
          {
            payload: payload,
            type: isFile ? 'INLINE' : 'URL',
            format: 'API',
            with_documentation: configsFormValue.importDocumentation,
            with_path_mapping: configsFormValue.importPathMapping,
            with_policies: this.policies.filter((policy) => configsFormValue.importPolicies[policy.id]).map((policy) => policy.id),
            with_policy_paths: configsFormValue.importPolicyPaths,
          },
          this.definitionVersion,
          this.updateModeApiId,
        );
        break;

      case 'GRAVITEE':
        importRequest$ = this.apiService.importApiDefinition(
          isFile ? 'graviteeJson' : 'graviteeUrl',
          payload,
          this.definitionVersion,
          this.updateModeApiId,
        );
        break;
    }

    importRequest$
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('API imported successfully')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message ?? 'An error occurred while importing the API');
          return EMPTY;
        }),
      )
      .subscribe((api) => {
        this.dialogRef.close(api.id);
      });
  }

  private resetImportFile(message?: string) {
    if (message) {
      this.snackBarService.error(message);
    }
    this.importFile = undefined;
    this.importType = undefined;
    this.filePickerValue = [];
  }
}

const getFileContent = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      resolve(reader.result as string);
    };
    reader.onerror = reject;
    reader.readAsText(file);
  });
};

const isSwaggerJsonContent = (fileContent: unknown): boolean => {
  return (
    fileContent instanceof Object &&
    (Object.prototype.hasOwnProperty.call(fileContent, 'swagger') ||
      Object.prototype.hasOwnProperty.call(fileContent, 'swaggerVersion') ||
      Object.prototype.hasOwnProperty.call(fileContent, 'openapi'))
  );
};
