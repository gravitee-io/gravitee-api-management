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
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { RulesetV2Service } from '../../../../services-ngx/ruleset-v2.service';
import { CreateRulesetRequestData, mapToRulesetFormat } from '../../../../entities/management-api-v2/api/v4/ruleset';

@Component({
  selector: 'import-api-score-ruleset',
  templateUrl: './import-api-score-ruleset.component.html',
  styleUrl: './import-api-score-ruleset.component.scss',
  standalone: false,
})
export class ImportApiScoreRulesetComponent implements OnInit {
  protected importType: string;
  public isLoading = false;

  public form: FormGroup = this.formBuilder.group({
    definitionFormat: ['', [Validators.required]],
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
    fileContent: ['', Validators.required],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly snackBarService: SnackBarService,
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly destroyRef: DestroyRef,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.handleDefinitionFormatChange();
  }

  get definitionFormatValue() {
    return this.form.get('definitionFormat') as FormControl;
  }

  get graviteeApiFormat() {
    return this.form.get('graviteeApiFormat') as FormControl;
  }

  public handleDefinitionFormatChange() {
    this.definitionFormatValue.valueChanges.subscribe((definitionFormat) => {
      if (definitionFormat === 'GraviteeAPI') {
        this.form.addControl('graviteeApiFormat', this.formBuilder.control('', [Validators.required]));
      } else {
        this.form.removeControl('graviteeApiFormat');
      }
    });
  }

  protected onImportFile({ importFileContent, importType }: { importFileContent: string; importType: string }) {
    this.importType = importType;
    this.form.patchValue({
      fileContent: importFileContent || '',
    });
    this.form.updateValueAndValidity();

    if (importFileContent !== undefined && this.form.get('fileContent').hasError('required')) {
      this.snackBarService.error('The file can not be empty');
    }
  }

  public importRuleset() {
    this.isLoading = true;

    let data: CreateRulesetRequestData = {
      name: this.form.value.name,
      description: this.form.value.description,
      payload: this.form.value.fileContent,
    };

    if (this.graviteeApiFormat?.value) {
      data = { ...data, format: mapToRulesetFormat(this.graviteeApiFormat.value) };
    }

    this.rulesetV2Service
      .createRuleset(data)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.snackBarService.success('Ruleset imported.');
          this.router.navigate(['..'], { relativeTo: this.activatedRoute });
        },
        error: () => {
          this.isLoading = false;
          this.snackBarService.error('Ruleset import error!');
        },
      });
  }
}
