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
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { TreeComponent } from './tree.component';

import { GioTestingModule } from '../../../shared/testing';
import {
  fakePortalNavigationFolder,
  fakePortalNavigationLink,
  fakePortalNavigationPage,
  PortalNavigationItem,
} from '../../../entities/management-api-v2';

describe('TreeComponent', () => {
  let fixture: ComponentFixture<TreeComponent>;
  let component: TreeComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreeComponent, MatIconTestingModule, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TreeComponent);
    component = fixture.componentInstance;
  });

  const makeItem = (
    id: string,
    type: 'PAGE' | 'FOLDER' | 'LINK',
    title: string,
    order?: number,
    parentId?: string | null,
  ): PortalNavigationItem => {
    switch (type) {
      case 'FOLDER':
        return fakePortalNavigationFolder({ id, title, order, parentId });
      case 'LINK':
        return fakePortalNavigationLink({ id, title, order, parentId });
      case 'PAGE':
      default:
        return fakePortalNavigationPage({ id, title, order, parentId });
    }
  };

  it('should build a sorted tree with proper types and parent/child relationships', () => {
    const links = [
      // root page with smallest order -> should be first
      makeItem('r1', 'PAGE', 'Root Page 1', 0),
      // folder root
      makeItem('f1', 'FOLDER', 'Folder 1', 1),
      // link root
      makeItem('l1', 'LINK', 'External Link', 3),
      // child page under folder
      makeItem('c1', 'PAGE', 'Child Page', 2, 'f1'),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const tree = component.tree();
    expect(Array.isArray(tree)).toBe(true);
    expect(tree.length).toBe(3);

    // roots sorted by order: r1 (0), f1 (1), l1 (3)
    expect(tree[0].id).toBe('r1');
    expect(tree[0].type).toBe('PAGE');
    expect(tree[0].children).toBeUndefined();

    expect(tree[1].id).toBe('f1');
    expect(tree[1].type).toBe('FOLDER');
    expect(tree[1].children?.length).toBe(1);
    expect(tree[1].children?.[0].id).toBe('c1');
    expect(tree[1].children?.[0].type).toBe('PAGE');
    expect(tree[1].children?.[0].children).toBeUndefined();

    expect(tree[2].id).toBe('l1');
    expect(tree[2].type).toBe('LINK');
    expect(tree[2].children).toBeUndefined();
  });

  it('should not auto-select when selectedId exists in tree', () => {
    const selectSpy = jest.fn();
    component.select.subscribe(selectSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', 'p1');
    fixture.detectChanges();

    // When selectedId exists, component should NOT emit default selection
    expect(selectSpy).not.toHaveBeenCalled();
  });
});
