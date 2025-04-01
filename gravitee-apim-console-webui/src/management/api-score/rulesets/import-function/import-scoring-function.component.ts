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
import { Component, DestroyRef, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { filter, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';

import { CreateFunctionRequestData } from '../../../../entities/management-api-v2/api/v4/ruleset';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { RulesetV2Service } from '../../../../services-ngx/ruleset-v2.service';

@Component({
  selector: 'import-scoring-function',
  templateUrl: './import-scoring-function.component.html',
  styleUrl: './import-scoring-function.component.scss',
  standalone: false,
})
export class ImportScoringFunctionComponent implements OnInit {
  protected importType: string;
  public isLoading = false;
  private importFileContent: string;
  private existingFunctionsNames: string[];

  public form: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/^[^/]+\.js$/)]],
    fileContent: ['', Validators.required],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly snackBarService: SnackBarService,
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.rulesetV2Service
      .listFunctions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.existingFunctionsNames = res.data.map((f) => f.name);
          this.isLoading = false;
        },
        error: () => {
          this.snackBarService.error('Functions error!');
        },
      });
  }

  protected onImportFile({
    importFileContent,
    importType,
    importFile,
  }: {
    importFileContent: string;
    importType: string;
    importFile: File;
  }) {
    this.importType = importType;
    this.importFileContent = importFileContent;
    this.form.patchValue({
      name: importFile?.name || '',
      fileContent: importFileContent || '',
    });

    this.form.updateValueAndValidity();

    if (importFileContent !== undefined && this.form.get('fileContent').hasError('required')) {
      this.snackBarService.error('The file can not be empty');
    }

    if (this.form.get('name').hasError('pattern')) {
      this.snackBarService.error('File name should fulfill ^[^/]+\\.js$ pattern');
    }

    if (this.form.get('name').hasError('maxlength')) {
      this.snackBarService.error('File name can not exceed 50 characters.');
    }
  }

  public isFunctionAlreadyImported(fileName: string): boolean {
    return this.existingFunctionsNames.includes(fileName);
  }

  public importFunction() {
    const data: CreateFunctionRequestData = {
      name: this.form.value.name,
      payload: this.importFileContent,
    };

    if (this.isFunctionAlreadyImported(data.name)) {
      this.overrideFunction(data);
    } else {
      this.importNewFunction(data);
    }
  }

  public importNewFunction(data: CreateFunctionRequestData) {
    this.isLoading = true;
    this.rulesetV2Service
      .createFunction(data)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.snackBarService.success('Function imported.');
          this.router.navigate(['..'], { relativeTo: this.activatedRoute });
        },
        error: () => {
          this.isLoading = false;
          this.snackBarService.error('Function import error!');
        },
      });
  }

  public overrideFunction(data: CreateFunctionRequestData) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Overwrite Function?',
          content:
            'A function with the same name already exists. Overwriting it will replace the existing function and its contents. This action cannot be undone. Are you sure you want to proceed?',
          confirmButton: 'Overwrite',
        },
        role: 'alertdialog',
        id: 'overwriteFunctionConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirm) => !!confirm),
        switchMap(() => {
          this.isLoading = true;
          return this.rulesetV2Service.createFunction(data);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.snackBarService.success('Function overwritten successfully!');
          this.router.navigate(['..'], { relativeTo: this.activatedRoute });
        },
        error: () => {
          this.isLoading = false;
          this.snackBarService.error('Function overwrite error!');
        },
      });
  }
}
