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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import '@gravitee/ui-components/wc/gv-schema-form';

import { DebugRequest } from '../../models/DebugRequest';

@Component({
  selector: 'policy-studio-debug-request',
  templateUrl: './policy-studio-debug-request.component.html',
  styleUrls: ['./policy-studio-debug-request.component.scss'],
  standalone: false,
})
export class PolicyStudioDebugRequestComponent implements OnInit {
  @Input()
  public debugInProgress = false;

  @Output()
  public requestSubmitted = new EventEmitter<DebugRequest>();

  @Output()
  public cancelSubmitted = new EventEmitter<void>();

  public httpMethods = ['GET', 'DELETE', 'PATCH', 'POST', 'PUT', 'OPTIONS', 'TRACE', 'HEAD'];

  public requestFormGroup: UntypedFormGroup;

  private unsubscribe$ = new Subject<boolean>();

  ngOnInit() {
    this.requestFormGroup = new UntypedFormGroup({
      method: new UntypedFormControl(this.httpMethods[0]),
      path: new UntypedFormControl('/'),
      headers: new UntypedFormControl([]),
      body: new UntypedFormControl(''),
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onBodyChange(value: string) {
    this.requestFormGroup.get('body').setValue(value ?? '');
  }

  onSendRequest() {
    this.requestSubmitted.emit(this.requestFormGroup.value as DebugRequest);
  }

  onCancelClick() {
    this.cancelSubmitted.emit();
  }
}
