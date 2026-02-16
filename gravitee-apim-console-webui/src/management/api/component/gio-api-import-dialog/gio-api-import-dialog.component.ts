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
import { Component, Inject, OnDestroy, signal, WritableSignal } from '@angular/core';
import { FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, startWith, takeUntil, tap } from 'rxjs/operators';

import { Api } from '../../../../entities/api';
import { PolicyListItem } from '../../../../entities/policy';
import { ApiService } from '../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';

export type GioApiImportDialogData = {
  policies?: PolicyListItem[];
  apiId?: string;
};

@Component({
  selector: 'gio-api-import-dialog',
  templateUrl: './gio-api-import-dialog.component.html',
  styleUrls: ['./gio-api-import-dialog.component.scss'],
  standalone: false,
})
export class GioApiImportDialogComponent implements OnDestroy {
  tabLabels = {
    UploadFile: 'Upload a file',
    SwaggerOpenAPI: 'Swagger / OpenAPI',
    ApiDefinition: 'API definition',
    WSDL: 'WSDL',
  };
  importType: string;
  policies = [];
  isUpdateMode = false;
  updateModeApiId?: string;
  importFile: File;
  importFileContent: string;
  descriptorUrlForm = new UntypedFormControl();
  configsForm: UntypedFormGroup;
  selectedTabIndex = 0;
  isImportingApi: WritableSignal<boolean> = signal(false);

  public get isImportValid(): boolean {
    return !!this.importType && (!!this.importFile || !!this.descriptorUrlForm?.value);
  }

  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly dialogRef: MatDialogRef<GioApiImportDialogData>,
    @Inject(MAT_DIALOG_DATA) dialogData: GioApiImportDialogData,
    private readonly apiService: ApiService,
    private readonly apiV2Service: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {
    this.policies = dialogData?.policies ?? [];
    this.isUpdateMode = !!dialogData?.apiId;
    this.updateModeApiId = dialogData?.apiId ?? undefined;

    this.configsForm = new UntypedFormGroup({
      importDocumentation: new UntypedFormControl(false),
      importPathMapping: new UntypedFormControl(false),
      importPolicyPaths: new UntypedFormControl(false),
      importPolicies: new UntypedFormGroup(
        this.policies.reduce((acc, policy) => {
          acc[policy.id] = new UntypedFormControl(false);
          return acc;
        }, {}),
      ),
    });

    // Unselect all policies when unselecting "Create flows on path"
    this.configsForm
      .get('importPolicyPaths')
      .valueChanges.pipe(startWith(this.configsForm.get('importPolicyPaths').value), takeUntil(this.unsubscribe$))
      .subscribe(checked => {
        Object.keys((this.configsForm.get('importPolicies') as FormGroup).controls).map(policyId => {
          checked
            ? this.configsForm.get('importPolicies').get(policyId).enable({ emitEvent: false })
            : this.configsForm.get('importPolicies').get(policyId).disable({ emitEvent: false });
        });

        this.configsForm.get('importPolicies').patchValue(
          this.policies.reduce((acc, policy) => {
            if (!checked) {
              acc[policy.id] = checked;
            }
            return acc;
          }, {}),
          { emitEvent: false },
        );
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onImportFile({ importFile, importFileContent, importType }: { importFile: File; importFileContent: string; importType: string }) {
    this.importType = importType;
    this.importFile = importFile;
    this.importFileContent = importFileContent;
  }

  onSelectedTab(event: MatTabChangeEvent) {
    this.importType = undefined;
    const initUrlDescriptor = () => {
      if (!this.descriptorUrlForm) {
        this.descriptorUrlForm = new UntypedFormControl('', [Validators.required]);
      }
    };

    switch (event.tab.textLabel) {
      case this.tabLabels.UploadFile:
        this.resetImportFile();
        this.descriptorUrlForm = new UntypedFormControl();
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

  async onImport() {
    const configsFormValue = this.configsForm.value;

    const isFile = !!this.importFile;
    const payload = isFile ? this.importFileContent : this.descriptorUrlForm.value;

    if (!this.importFile && !this.descriptorUrlForm.value) {
      // Crazy guard. should not happen
      throw new Error('No file or url provided');
    }

    this.isImportingApi.set(true);

    let importRequest$: Observable<Api | ApiV4>;

    switch (this.importType) {
      case 'WSDL':
        importRequest$ = this.apiService.importSwaggerApi(
          {
            payload: payload,
            type: isFile ? 'INLINE' : 'URL',
            format: 'WSDL',
            with_documentation: configsFormValue.importDocumentation,
            with_path_mapping: configsFormValue.importPathMapping,
            with_policies: this.policies
              .filter(policy => configsFormValue.importPolicies && configsFormValue.importPolicies[policy.id])
              .map(policy => policy.id),
            with_policy_paths: configsFormValue.importPolicyPaths,
          },
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
            with_policies: this.policies
              .filter(policy => configsFormValue.importPolicies && configsFormValue.importPolicies[policy.id])
              .map(policy => policy.id),
            with_policy_paths: configsFormValue.importPolicyPaths,
          },
          this.updateModeApiId,
        );
        break;

      case 'GRAVITEE':
        importRequest$ = this.apiService.importApiDefinition(isFile ? 'graviteeJson' : 'graviteeUrl', payload, this.updateModeApiId);
        break;

      case 'MAPI_V2':
        importRequest$ = this.apiV2Service.import(payload);
        break;
    }

    importRequest$
      .pipe(
        tap(() => this.snackBarService.success('API imported successfully')),
        catchError(({ error }) => {
          this.isImportingApi.set(false);
          this.snackBarService.error(error.message ?? 'An error occurred while importing the API');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(api => {
        this.dialogRef.close(api.id);
      });
  }

  private resetImportFile(message?: string) {
    if (message) {
      this.snackBarService.error(message);
    }
    this.importFile = undefined;
    this.importType = undefined;
  }
}
