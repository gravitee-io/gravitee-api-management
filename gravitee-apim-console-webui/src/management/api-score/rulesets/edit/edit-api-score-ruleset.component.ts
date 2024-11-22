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
import { switchMap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { RulesetV2Service } from '../../../../services-ngx/ruleset-v2.service';
import { EditRulesetRequestData } from '../../../../entities/management-api-v2/api/v4/ruleset';

@Component({
  selector: 'edit-api-score-ruleset',
  templateUrl: './edit-api-score-ruleset.component.html',
  styleUrl: './edit-api-score-ruleset.component.scss',
})
export class EditApiScoreRulesetComponent implements OnInit {
  public isLoading = false;
  public filePreview = null;

  public form: FormGroup = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50), Validators.minLength(1)]],
    description: ['', Validators.maxLength(250)],
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly snackBarService: SnackBarService,
    private readonly rulesetV2Service: RulesetV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly destroyRef: DestroyRef,
  ) {}

  ngOnInit() {
    this.rulesetV2Service
      .getRuleset(this.activatedRoute.snapshot.params.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.form.patchValue({
            name: res.name,
            description: res.description,
          });
          this.filePreview = res.payload;
        },
        error: () => {
          this.snackBarService.error('Ruleset error');
          return EMPTY;
        },
      });
  }

  public edit() {
    this.isLoading = true;
    const data: EditRulesetRequestData = {
      name: this.form.value.name,
      description: this.form.value.description,
    };

    this.rulesetV2Service
      .editRuleset(this.activatedRoute.snapshot.params.id, data)
      .pipe(switchMap(() => this.rulesetV2Service.getRuleset(this.activatedRoute.snapshot.params.id)))
      .subscribe({
        next: (res) => {
          this.form.patchValue({
            name: res.name,
            description: res.description,
          });
          this.isLoading = false;
          this.snackBarService.success('Ruleset updated.');
          this.form.markAsPristine();
        },
        error: () => {
          this.isLoading = false;
          this.snackBarService.error('Ruleset update error!');
        },
      });
  }
}
