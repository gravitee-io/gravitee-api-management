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
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  WritableSignal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { AbstractControl, FormControl, FormGroup, FormsModule, ReactiveFormsModule, ValidationErrors, ValidatorFn } from '@angular/forms';
import { GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Group, Page, PageType, Visibility } from '../../../../../entities/management-api-v2';
import { ApiDocumentationV4VisibilityComponent } from '../api-documentation-v4-visibility/api-documentation-v4-visibility.component';

export interface PageConfigurationForm {
  name: FormControl<string>;
  visibility: FormControl<Visibility>;
  accessControlGroups: FormControl<string[]>;
  excludeGroups: FormControl<boolean>;
}

@Component({
  selector: 'api-documentation-v4-page-configuration',
  templateUrl: './api-documentation-v4-page-configuration.component.html',
  styleUrls: ['./api-documentation-v4-page-configuration.component.scss'],
  imports: [
    MatButtonModule,
    ApiDocumentationV4VisibilityComponent,
    FormsModule,
    GioFormSlideToggleModule,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatOption,
    MatSelect,
    MatSlideToggle,
    ReactiveFormsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiDocumentationV4PageConfigurationComponent implements OnInit, OnChanges {
  @Input()
  name: WritableSignal<string>;

  @Input()
  pageType: PageType;

  @Input()
  form!: FormGroup<PageConfigurationForm>;

  @Input()
  groups: Group[] = [];

  @Input()
  apiPages: Page[] = [];

  @Input()
  pageId?: string;

  @Input()
  homepage?: boolean;

  private existingNames: string[] = [];
  private destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.setExistingNames();

    this.form.controls.name.addValidators([this.pageNameUniqueValidator()]);

    this.form.controls.name.valueChanges
      .pipe(
        tap((val) => this.name.set(val)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.form.controls.accessControlGroups.valueChanges
      .pipe(
        tap((value) => {
          if (value.length === 0) {
            this.form.controls.excludeGroups.setValue(false);
            this.form.controls.excludeGroups.disable();
          } else {
            this.form.controls.excludeGroups.enable();
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.apiPages) {
      this.setExistingNames();
    }
  }

  private pageNameUniqueValidator(): ValidatorFn {
    return (nameControl: AbstractControl): ValidationErrors | null =>
      this.existingNames.includes(nameControl.value?.toLowerCase().trim()) ? { unique: true } : null;
  }

  private setExistingNames(): void {
    this.existingNames = this.apiPages
      .filter((page) => page.type === this.pageType && (!this.pageId || page.id !== this.pageId))
      .map((page) => page.name.toLowerCase().trim());
  }
}
