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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { OpenApiConfigDialogComponent } from './openapi-config-dialog.component';

import { OpenApiDocExpansion, OpenApiViewer } from '../../../entities/management-api-v2/portalPageContent/openApiViewerConfiguration';

describe('OpenApiConfigDialogComponent', () => {
  let fixture: ComponentFixture<OpenApiConfigDialogComponent>;
  let component: OpenApiConfigDialogComponent;
  let dialogRefClose: jest.Mock;

  beforeEach(async () => {
    dialogRefClose = jest.fn();

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, OpenApiConfigDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            configuration: {
              viewer: OpenApiViewer.Swagger,
              tryItURL: 'https://api.example.com',
              displayOperationId: true,
              docExpansion: OpenApiDocExpansion.Full,
              enableFiltering: true,
              maxDisplayedTags: 10,
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OpenApiConfigDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should emit camelCase configuration keys on save', () => {
    component.form.patchValue({
      viewer: OpenApiViewer.Redoc,
      tryItURL: 'https://try-it.example.com',
      tryIt: true,
      disableSyntaxHighlight: true,
      tryItAnonymous: true,
      showURL: true,
      displayOperationId: true,
      usePkce: true,
      docExpansion: OpenApiDocExpansion.List,
      enableFiltering: true,
      showExtensions: true,
      showCommonExtensions: true,
      maxDisplayedTags: 12,
    });

    component.onSave();

    expect(dialogRefClose).toHaveBeenCalledWith({
      viewer: OpenApiViewer.Redoc,
      tryItURL: 'https://try-it.example.com',
      tryIt: true,
      disableSyntaxHighlight: true,
      tryItAnonymous: true,
      showURL: true,
      displayOperationId: true,
      usePkce: true,
      docExpansion: OpenApiDocExpansion.List,
      enableFiltering: true,
      showExtensions: true,
      showCommonExtensions: true,
      maxDisplayedTags: 12,
    });
  });
});
