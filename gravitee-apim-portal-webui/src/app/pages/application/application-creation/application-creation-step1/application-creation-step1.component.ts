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
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-application-creation-step1',
  templateUrl: './application-creation-step1.component.html',
  styleUrls: ['../application-creation.component.css'],
})
export class ApplicationCreationStep1Component implements OnInit {
  form: UntypedFormGroup;

  @Output() updated = new EventEmitter<UntypedFormGroup>();

  constructor(private formBuilder: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      name: new UntypedFormControl(null, [Validators.required]),
      description: new UntypedFormControl(null, [Validators.required]),
      domain: new UntypedFormControl(null),
      picture: new UntypedFormControl(null),
    });

    this.form.valueChanges.subscribe(() => {
      this.updated.emit(this.form);
    });
    setTimeout(() => {
      this.updated.emit(this.form);
    });
  }
}
