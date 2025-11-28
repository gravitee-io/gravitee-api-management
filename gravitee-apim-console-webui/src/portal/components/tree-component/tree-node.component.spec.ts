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
import { By } from '@angular/platform-browser';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { TreeNodeComponent } from './tree-node.component';
import { SectionNode } from './tree.component';

import { GioTestingModule } from '../../../shared/testing';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TreeNodeComponent>;
  let component: TreeNodeComponent;

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
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TreeNodeComponent);
    component = fixture.componentInstance;
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
});
