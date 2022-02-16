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
import { FormControl, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import '@gravitee/ui-components/wc/gv-schema-form';

import { CorsUtil } from '../../../../../../shared/utils';
import { DebugRequest } from '../../models/DebugRequest';

@Component({
  selector: 'policy-studio-debug-request',
  template: require('./policy-studio-debug-request.component.html'),
  styles: [require('./policy-studio-debug-request.component.scss')],
})
export class PolicyStudioDebugRequestComponent implements OnInit {
  @Input()
  public debugInProgress = false;

  @Output()
  public requestSubmitted = new EventEmitter<DebugRequest>();

  @Output()
  public cancelSubmitted = new EventEmitter<void>();

  public httpMethods = CorsUtil.httpMethods;

  public requestFormGroup: FormGroup;

  public headersControl = {
    type: 'object',
    id: 'urn:jsonschema:io:gravitee:debug:request:headers',
    properties: {
      headers: {
        type: 'array',
        title: ' ',
        items: {
          type: 'object',
          title: 'Header',
          properties: {
            name: {
              title: 'Name',
              type: 'string',
            },
            value: {
              title: 'Value',
              type: 'string',
            },
          },
        },
        required: ['name', 'value'],
      },
    },
  };

  private unsubscribe$ = new Subject<boolean>();

  ngOnInit() {
    this.requestFormGroup = new FormGroup({
      method: new FormControl(this.httpMethods[0]),
      path: new FormControl('/'),
      headers: new FormControl([]),
      body: new FormControl(''),
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onHeadersChange({ values }: { values: { headers?: { name?: string; value?: string }[] } }) {
    this.requestFormGroup.get('headers').setValue(values.headers ?? []);
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
