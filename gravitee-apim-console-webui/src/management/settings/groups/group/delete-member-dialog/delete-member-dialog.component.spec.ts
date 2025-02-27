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
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { DeleteMemberDialogComponent } from './delete-member-dialog.component';

import { GioTestingModule } from '../../../../../shared/testing';

describe('DeleteMemberDialogComponent', () => {
  TestBed.configureTestingModule({
    imports: [DeleteMemberDialogComponent, GioTestingModule],
    declarations: [],
    providers: [
      {
        provide: MatDialogRef,
        useValue: {
          close: jest.fn(),
        },
      },
      {
        provide: MAT_DIALOG_DATA,
        useValue: {
          /* mock data */
        },
      },
    ],
  }).compileComponents();

  it('should create', () => {
    const fixture = TestBed.createComponent(DeleteMemberDialogComponent);
    const component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });
});
