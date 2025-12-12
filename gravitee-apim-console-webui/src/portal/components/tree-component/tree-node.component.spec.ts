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
import { By, ɵSharedStylesHost as SharedStylesHost } from '@angular/platform-browser';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatMenuHarness, MatMenuItemHarness } from '@angular/material/menu/testing';

import { TreeNodeComponent } from './tree-node.component';
import { SectionNode } from './tree.component';

import { GioTestingModule } from '../../../shared/testing';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
// Angular's JSDOM environment (used by Jest) currently cannot parse some modern CSS constructs
// like `@layer` used by Angular CDK. When Material menus/overlays load, the CDK injects styles
// into <style> tags and jsdom tries (and fails) to parse them, producing noisy errors.
// To avoid this in unit tests, we provide a no-op SharedStylesHost so no styles are injected.
// This relies on Angular's internal symbol which is acceptable in tests.

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TreeNodeComponent>;
  let component: TreeNodeComponent;
  let rootLoader: HarnessLoader;

  const baseNode: SectionNode = {
    id: 'n1',
    label: 'Node 1',
    type: 'PAGE',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreeNodeComponent, MatIconTestingModule, NoopAnimationsModule, GioTestingModule],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(true),
          },
        },
        // Prevent Angular from injecting style tags (which jsdom can't parse due to `@layer`).
        {
          provide: SharedStylesHost,
          useValue: {
            // no-op implementations used by Angular internally
            addStyles: () => {},
            addStyle: () => {},
            addHost: () => {},
            removeHost: () => {},
            ngOnDestroy: () => {},
            onStylesAdded: () => {},
            addUsage: () => {},
            removeUsage: () => {},
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TreeNodeComponent);
    component = fixture.componentInstance;
    rootLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.componentRef.setInput('node', baseNode);
    fixture.componentRef.setInput('level', 0);
    fixture.componentRef.setInput('selectedId', null);
    fixture.detectChanges();
  });

  it('should render basic node with label and icon', () => {
    const labelBtn = fixture.debugElement.query(By.css('.tree__label'));
    expect(labelBtn.nativeElement.textContent.trim()).toBe('Node 1');

    const icon = fixture.debugElement.query(By.css('.tree__icon'));
    // icon is rendered with svgIcon="gio:page"
    expect(icon.attributes['ng-reflect-svg-icon'] || icon.attributes['svgicon'] || '').toContain('page');
  });

  it('should compute selected state from selectedId input', () => {
    const row = fixture.debugElement.query(By.css('.tree__row'));
    expect(row.nativeElement.classList.contains('selected')).toBe(false);

    fixture.componentRef.setInput('selectedId', 'n1');
    fixture.detectChanges();

    expect(row.nativeElement.classList.contains('selected')).toBe(true);
    expect(row.attributes['aria-selected']).toBe('true');
  });

  it('should emit nodeSelected when clicking row', () => {
    const spy = jest.fn();
    component.nodeSelected.subscribe(spy);

    // page nodes do not have a toggle button
    const toggleBtn = fixture.debugElement.query(By.css('.tree__button__expand'));
    expect(toggleBtn).toBeNull();

    const labelBtn = fixture.debugElement.query(By.css('.tree__row'));
    labelBtn.triggerEventHandler('click');
    fixture.detectChanges();

    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith(baseNode);
  });

  it('should toggle expanded state on folders and show/hide children', () => {
    const folderNode: SectionNode = {
      id: 'f1',
      label: 'Folder 1',
      type: 'FOLDER',
      children: [baseNode],
    };

    fixture.componentRef.setInput('node', folderNode);
    fixture.detectChanges();

    const row = fixture.debugElement.query(By.css('.tree__row'));
    // default is expanded
    expect(row.attributes['aria-expanded']).toBe('true');
    expect(fixture.debugElement.query(By.css('.tree__children'))).toBeTruthy();

    const toggleBtn = fixture.debugElement.query(By.css('.tree__icon[data-test-icon="toggle"]'));
    expect(toggleBtn).toBeTruthy();

    // click to collapse
    toggleBtn.triggerEventHandler('click', { stopPropagation: jest.fn() });
    fixture.detectChanges();
    expect(row.attributes['aria-expanded']).toBe('false');
    expect(fixture.debugElement.query(By.css('.tree__children'))).toBeNull();

    // click again to expand
    toggleBtn.triggerEventHandler('click', { stopPropagation: jest.fn() });
    fixture.detectChanges();
    expect(row.attributes['aria-expanded']).toBe('true');
    expect(fixture.debugElement.query(By.css('.tree__children'))).toBeTruthy();
  });

  it('should propagate selectedId and events to children when expanded', () => {
    const child: SectionNode = { id: 'c1', label: 'Child', type: 'PAGE' };
    const folderNode: SectionNode = { id: 'f2', label: 'Folder 2', type: 'FOLDER', children: [child] };

    fixture.componentRef.setInput('node', folderNode);
    fixture.componentRef.setInput('selectedId', 'c1');
    fixture.detectChanges();

    // already expanded by default; no need to click toggle

    const childRow = fixture.debugElement.queryAll(By.css('.tree__row'))[1];
    // child should be selected
    expect(childRow.nativeElement.classList.contains('selected')).toBe(true);

    // clicking on child should bubble through outputs
    const selectedSpy = jest.fn();
    component.nodeSelected.subscribe(selectedSpy);

    const childLabel = fixture.debugElement.queryAll(By.css('.tree__row'))[1];
    childLabel.triggerEventHandler('click');
    fixture.detectChanges();

    expect(selectedSpy).toHaveBeenCalledWith(child);
  });

  it('should have a more actions button', () => {
    const moreBtn = fixture.debugElement.query(By.css('.tree__button.more-actions'));
    expect(moreBtn).toBeTruthy();
    expect(moreBtn.nativeElement.getAttribute('aria-label')).toBe('More actions');
  });

  it('should display add page/folder/link menu items for folder and emit corresponding actions', async () => {
    const folderNode: SectionNode = {
      id: 'f3',
      label: 'Folder 3',
      type: 'FOLDER',
    };

    fixture.componentRef.setInput('node', folderNode);
    fixture.detectChanges();

    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    // Open the Angular Material menu via the trigger (renders in overlay)
    const triggerBtn = fixture.debugElement.query(By.css('.tree__button.more-actions'));
    expect(triggerBtn).toBeTruthy();
    triggerBtn.nativeElement.click();
    fixture.detectChanges();
    await fixture.whenStable();

    // Assert items are present in the overlay and click them using harnesses
    const menu = await rootLoader.getHarness(MatMenuHarness);
    expect(menu).toBeTruthy();

    const addPageItem = await rootLoader.getHarness(MatMenuItemHarness.with({ text: /Add Page/i }));
    const addFolderItem = await rootLoader.getHarness(MatMenuItemHarness.with({ text: /Add Folder/i }));
    const addLinkItem = await rootLoader.getHarness(MatMenuItemHarness.with({ text: /Add Link/i }));

    expect(addPageItem).toBeTruthy();
    expect(addFolderItem).toBeTruthy();
    expect(addLinkItem).toBeTruthy();

    await addPageItem.click();
    await addFolderItem.click();
    await addLinkItem.click();
    fixture.detectChanges();

    expect(actionSpy).toHaveBeenCalledTimes(3);
    expect(actionSpy).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({
        action: 'create',
        itemType: 'PAGE',
        node: folderNode,
      }),
    );
    expect(actionSpy).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({
        action: 'create',
        itemType: 'FOLDER',
        node: folderNode,
      }),
    );
    expect(actionSpy).toHaveBeenNthCalledWith(
      3,
      expect.objectContaining({
        action: 'create',
        itemType: 'LINK',
        node: folderNode,
      }),
    );
  });

  it('should show disabled Delete menu item when folder has children', async () => {
    const folderNode: SectionNode = {
      id: 'f-child',
      label: 'Folder With Children',
      type: 'FOLDER',
      children: [baseNode],
    };

    fixture.componentRef.setInput('node', folderNode);
    fixture.detectChanges();

    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const triggerBtn = fixture.debugElement.query(By.css('.tree__button.more-actions'));
    expect(triggerBtn).toBeTruthy();
    triggerBtn.nativeElement.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const deleteItem = await rootLoader.getHarness(MatMenuItemHarness.with({ text: /Delete/i }));
    expect(deleteItem).toBeTruthy();
    expect(await deleteItem.isDisabled()).toBe(true);

    // clicking disabled item should not emit action
    await expect(deleteItem.click()).resolves.toBeUndefined();
    expect(actionSpy).not.toHaveBeenCalled();
  });

  it('should enable Delete menu item when folder has no children and emit delete action', async () => {
    const emptyFolder: SectionNode = {
      id: 'f-empty',
      label: 'Empty Folder',
      type: 'FOLDER',
      children: [],
    };

    fixture.componentRef.setInput('node', emptyFolder);
    fixture.detectChanges();

    const actionSpy = jest.fn();
    component.nodeMenuAction.subscribe(actionSpy);

    const triggerBtn = fixture.debugElement.query(By.css('.tree__button.more-actions'));
    triggerBtn.nativeElement.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const deleteItem = await rootLoader.getHarness(MatMenuItemHarness.with({ text: /Delete/i }));
    expect(await deleteItem.isDisabled()).toBe(false);

    await deleteItem.click();
    fixture.detectChanges();

    expect(actionSpy).toHaveBeenCalledTimes(1);
    expect(actionSpy).toHaveBeenCalledWith(expect.objectContaining({ action: 'delete', itemType: 'FOLDER' }));
  });

  it('should render correct icons for node type and toggle state', () => {
    // For PAGE node, type icon should be gio:page
    let typeIcon = fixture.debugElement.query(By.css('.tree__icon.tree__icon__type'));
    expect(typeIcon.attributes['ng-reflect-svg-icon'] || typeIcon.attributes['svgicon'] || '').toContain('gio:page');

    // Switch to FOLDER node
    const folderNode: SectionNode = {
      id: 'f4',
      label: 'Folder 4',
      type: 'FOLDER',
      children: [],
    };
    fixture.componentRef.setInput('node', folderNode);
    fixture.detectChanges();

    typeIcon = fixture.debugElement.query(By.css('.tree__icon.tree__icon__type'));
    expect(typeIcon.attributes['ng-reflect-svg-icon'] || typeIcon.attributes['svgicon'] || '').toContain('gio:folder');

    const toggleIcon = fixture.debugElement.query(By.css('.tree__icon[data-test-icon="toggle"]'));
    // Expanded by default -> down icon
    expect(toggleIcon.attributes['ng-reflect-svg-icon'] || toggleIcon.attributes['svgicon'] || '').toContain('gio:nav-arrow-down');

    // Collapse -> right icon
    toggleIcon.triggerEventHandler('click', { stopPropagation: jest.fn() });
    fixture.detectChanges();
    const toggleIconCollapsed = fixture.debugElement.query(By.css('.tree__icon[data-test-icon="toggle"]'));
    expect(toggleIconCollapsed.attributes['ng-reflect-svg-icon'] || toggleIconCollapsed.attributes['svgicon'] || '').toContain(
      'gio:nav-arrow-right',
    );
  });
});
