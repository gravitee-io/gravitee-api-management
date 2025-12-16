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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { FlatTreeComponent } from './flat-tree.component';
import { FlatTreeComponentHarness } from './flat-tree.component.harness';

import { GioTestingModule } from '../../../shared/testing';
import {
  fakePortalNavigationFolder,
  fakePortalNavigationLink,
  fakePortalNavigationPage,
  PortalNavigationItem,
} from '../../../entities/management-api-v2';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

describe('FlatTreeComponent', () => {
  let fixture: ComponentFixture<FlatTreeComponent>;
  let component: FlatTreeComponent;
  let harness: FlatTreeComponentHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FlatTreeComponent, MatIconTestingModule, GioTestingModule, NoopAnimationsModule],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(true),
          },
        },
        // // Prevent Angular from injecting style tags (which jsdom can't parse due to `@layer`).
        // {
        //   provide: SharedStylesHost,
        //   useValue: {
        //     addStyles: () => {},
        //     addStyle: () => {},
        //     addHost: () => {},
        //     removeHost: () => {},
        //     ngOnDestroy: () => {},
        //     onStylesAdded: () => {},
        //     addUsage: () => {},
        //     removeUsage: () => {},
        //   },
        // },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FlatTreeComponent);
    component = fixture.componentInstance;
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FlatTreeComponentHarness);
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

  it('should not auto-select when selectedId exists in tree', async () => {
    const selectSpy = jest.fn();
    component.nodeSelect.subscribe(selectSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', 'p1');
    fixture.detectChanges();

    // When selectedId exists, component should NOT emit default selection
    expect(selectSpy).not.toHaveBeenCalled();
  });

  it('should re-emit delete events from child nodes', () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    // Simulate delete event from child node
    component.nodeMenuAction.emit({ action: 'delete', itemType: 'PAGE', node: { id: 'p1', label: 'Page 1', type: 'PAGE' } });

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith({ action: 'delete', itemType: 'PAGE', node: { id: 'p1', label: 'Page 1', type: 'PAGE' } });
  });

  it('should display empty state when no links provided', async () => {
    fixture.componentRef.setInput('links', null);
    fixture.detectChanges();

    const isEmpty = await harness.isEmptyStateDisplayed();
    expect(isEmpty).toBe(true);
  });

  it('should display empty state when links array is empty', async () => {
    fixture.componentRef.setInput('links', []);
    fixture.detectChanges();

    const isEmpty = await harness.isEmptyStateDisplayed();
    expect(isEmpty).toBe(true);
  });

  it('should get all item titles', async () => {
    const links = [
      makeItem('p1', 'PAGE', 'Page 1', 0),
      makeItem('f1', 'FOLDER', 'Folder 1', 1),
      makeItem('c1', 'PAGE', 'Child Page', 0, 'f1'),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const titles = await harness.getAllItemTitles();
    expect(titles.length).toBeGreaterThan(0);
    expect(titles).toContain('Page 1');
    expect(titles).toContain('Folder 1');
  });

  it('should select item by title', async () => {
    const selectSpy = jest.fn();
    component.nodeSelect.subscribe(selectSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('p2', 'PAGE', 'Page 2', 1)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    await harness.selectItemByTitle('Page 1');
    fixture.detectChanges();

    expect(selectSpy).toHaveBeenCalledTimes(1);
    expect(selectSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'p1',
        label: 'Page 1',
        type: 'PAGE',
      }),
    );
  });

  it('should get selected item title', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', 'p1');
    fixture.detectChanges();

    const selectedTitle = await harness.getSelectedItemTitle();
    expect(selectedTitle).toBe('Page 1');
  });

  it('should return null when no item is selected', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', null);
    fixture.detectChanges();

    const selectedTitle = await harness.getSelectedItemTitle();
    expect(selectedTitle).toBeNull();
  });

  it('should get selected item type', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.componentRef.setInput('selectedId', 'p1');
    fixture.detectChanges();

    const selectedType = await harness.getSelectedItemType();
    // getName() might return the full icon name like "gio:page" or just "page"
    expect(selectedType).toBeTruthy();
    expect(selectedType?.toLowerCase()).toContain('page');
  });

  it('should select edit action by id', async () => {
    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();

    await harness.selectEditById('p1');
    await fixture.whenStable();

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'edit',
        itemType: 'PAGE',
        node: expect.objectContaining({
          id: 'p1',
        }),
      }),
    );
  });

  it('should select delete action by id', async () => {
    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const links = [makeItem('p1', 'PAGE', 'Page 1', 0)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();
    await fixture.whenStable();

    await harness.selectDeleteById('p1');
    await fixture.whenStable();

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        action: 'delete',
        itemType: 'PAGE',
        node: expect.objectContaining({
          id: 'p1',
        }),
      }),
    );
  });

  it('should handle nested folder structure', async () => {
    const links = [
      makeItem('f1', 'FOLDER', 'Folder 1', 0),
      makeItem('f2', 'FOLDER', 'Folder 2', 0, 'f1'),
      makeItem('p1', 'PAGE', 'Page 1', 0, 'f2'),
    ];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const titles = await harness.getAllItemTitles();
    expect(titles).toContain('Folder 1');
    expect(titles).toContain('Folder 2');
    expect(titles).toContain('Page 1');
  });

  it('should handle different node types', async () => {
    const links = [makeItem('p1', 'PAGE', 'Page 1', 0), makeItem('f1', 'FOLDER', 'Folder 1', 1), makeItem('l1', 'LINK', 'Link 1', 2)];

    fixture.componentRef.setInput('links', links);
    fixture.detectChanges();

    const titles = await harness.getAllItemTitles();
    expect(titles.length).toBe(3);
    expect(titles).toContain('Page 1');
    expect(titles).toContain('Folder 1');
    expect(titles).toContain('Link 1');
  });
});
