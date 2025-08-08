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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';

import { ApiGeneralInfoMigrateToV4DialogComponent } from './api-general-info-migrate-to-v4-dialog.component';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

describe('ApiGeneralInfoMigrateToV4Component', () => {
  let component: ApiGeneralInfoMigrateToV4DialogComponent;
  let fixture: ComponentFixture<ApiGeneralInfoMigrateToV4DialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiGeneralInfoMigrateToV4DialogComponent, NoopAnimationsModule, MatIconTestingModule],
      providers: [
        {
          provide: ApiV2Service,
          useValue: {
            migrateToV4: jest.fn().mockReturnValue(of({ state: 'MIGRATABLE', issues: [] })),
          },
        },
        { provide: MAT_DIALOG_DATA, useValue: { apiId: 'api-id' } },
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiGeneralInfoMigrateToV4DialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
