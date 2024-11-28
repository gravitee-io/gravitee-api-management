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
import { Component, DestroyRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { CreateFunctionRequestData } from '../../../../entities/management-api-v2/api/v4/ruleset';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { RulesetV2Service } from '../../../../services-ngx/ruleset-v2.service';

@Component({
  selector: 'import-scoring-function',
  templateUrl: './import-scoring-function.component.html',
  styleUrl: './import-scoring-function.component.scss',
})
export class ImportScoringFunctionComponent {
  protected importType: string;
  public isLoading = false;
  private importFileContent: string;

  public form: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required]],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly snackBarService: SnackBarService,
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

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
      name: importFile.name,
    });
    this.form.updateValueAndValidity();
  }

  public importFunction() {
    this.isLoading = true;

    const data: CreateFunctionRequestData = {
      name: this.form.value.name,
      payload: this.importFileContent,
    };
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
}
