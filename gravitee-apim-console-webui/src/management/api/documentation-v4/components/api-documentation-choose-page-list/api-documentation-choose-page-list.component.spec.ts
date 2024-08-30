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
import { By } from '@angular/platform-browser';
import { MatTreeModule } from '@angular/material/tree';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApiDocumentationChoosePageListComponent, PageData } from './api-documentation-choose-page-list.component';

import { Page } from '../../../../../entities/management-api-v2';

describe('ApiDocumentationChoosePageListComponent', () => {
  let component: ApiDocumentationChoosePageListComponent;
  let fixture: ComponentFixture<ApiDocumentationChoosePageListComponent>;

  const pages: Page[] = [
    { id: 'page-id', name: 'Page 1', type: 'MARKDOWN', published: true, visibility: 'PUBLIC', generalConditions: false, parentId: null },
    {
      id: 'folder-id',
      name: 'Folder 1',
      type: 'FOLDER',
      published: false,
      visibility: 'PRIVATE',
      generalConditions: false,
      parentId: null,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        MatTreeModule,
        MatRadioModule,
        MatIconModule,
        MatTooltipModule,
        FormsModule,
        ApiDocumentationChoosePageListComponent,
      ],
      providers: [PageData],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationChoosePageListComponent);
    component = fixture.componentInstance;
    component.pages = pages;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the page list', () => {
    const matTreeElement = fixture.debugElement.query(By.css('mat-tree'));
    expect(matTreeElement).toBeTruthy();
  });

  it('should render both page types', () => {
    component.pages = pages;

    component.ngOnChanges({
      pages: {
        currentValue: pages,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    fixture.detectChanges();

    const matTreeNodes = fixture.debugElement.queryAll(By.css('mat-tree-node'));
    expect(matTreeNodes.length).toBe(2);

    const markdownNode = matTreeNodes.find((node) => node.nativeElement.textContent.includes('Page 1'));
    const folderNode = matTreeNodes.find((node) => node.nativeElement.textContent.includes('Folder 1'));

    expect(markdownNode).toBeTruthy();
    expect(folderNode).toBeTruthy();
  });

  it('should not render both page types if pages are empty', () => {
    component.pages = [];
    fixture.detectChanges();

    component.ngOnChanges({
      pages: {
        currentValue: pages,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    fixture.detectChanges();

    const matTreeNodes = fixture.debugElement.queryAll(By.css('mat-tree-node'));
    expect(matTreeNodes.length).toBe(0);
  });
});
