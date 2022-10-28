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
import { ChangeDetectorRef, Component, Inject, OnDestroy } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { NewFile } from '@gravitee/ui-particles-angular';
import { EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { PolicyListItem } from '../../../entities/policy';
import { ApiService } from '../../../services-ngx/api.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

const allowedFileExtensions = ['yml', 'yaml', 'json', 'wsdl', 'xml'] as const;

type FileExtension = typeof allowedFileExtensions[number];

export type GioApiImportDialogData = {
  policies?: PolicyListItem[];
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

  displayImportConfig: boolean = false;

  policies = [];

  filePickerValue = [];
  importFile: File;
  importFileContent: string;

  configsForm: FormGroup;
  importPolicyPathsIntermediate = false;

  public get isImportValid(): boolean {
    return !!this.importType;
  }

  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly dialogRef: MatDialogRef<GioApiImportDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: GioApiImportDialogData,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
  ) {
    this.policies = dialogData?.policies ?? [];

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
          console.log('aaa', {fileContent}, {error});
          
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
    switch (event.tab.textLabel) {
      case this.tabLabels.UploadFile:
        this.importType = undefined;
        // Defined after file upload
        break;

      case this.tabLabels.SwaggerOpenAPI:
        this.importType = 'SWAGGER';
        break;

      case this.tabLabels.ApiDefinition:
        this.importType = 'GRAVITEE';
        break;

      case this.tabLabels.WSDL:
        this.importType = 'WSDL';
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

    let importRequest$;

    switch (this.importType) {
      case 'WSDL':
        importRequest$ = this.apiService.importSwaggerApi(
          {
            payload: this.importFileContent,
            format: 'WSDL',
            type: 'INLINE',
            with_documentation: configsFormValue.importDocumentation,
            with_path_mapping: configsFormValue.importPathMapping,
            with_policies: this.policies.filter((policy) => configsFormValue.importPolicies[policy.id]).map((policy) => policy.id),
            with_policy_paths: configsFormValue.importPolicyPaths,
          },
          '2.0.0',
        );
        break;

      case 'SWAGGER':
        importRequest$ = this.apiService.importSwaggerApi(
          {
            payload: this.importFileContent,
            format: 'API',
            type: 'INLINE',
            with_documentation: configsFormValue.importDocumentation,
            with_path_mapping: configsFormValue.importPathMapping,
            with_policies: this.policies.filter((policy) => configsFormValue.importPolicies[policy.id]).map((policy) => policy.id),
            with_policy_paths: configsFormValue.importPolicyPaths,
          },
          '2.0.0',
        );
        break;

      case 'GRAVITEE':
        importRequest$ = this.apiService.importApiDefinition('graviteeJson', this.importFileContent, '2.0.0');
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
      .subscribe(() => {
        // this.dialogRef.close();
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
  return fileContent.hasOwnProperty('swagger') || fileContent.hasOwnProperty('swaggerVersion') || fileContent.hasOwnProperty('openapi');
};
