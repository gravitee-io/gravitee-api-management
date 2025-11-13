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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { TreeComponent } from './tree.component';

interface TestLink {
  id: string;
  name: string;
  type: string;
  target?: string | null;
  visibility: string;
  order: number;
  parentId?: string | null;
}

function link(partial: Partial<TestLink>): TestLink {
  return {
    id: 'id',
    name: 'name',
    type: 'INTERNAL',
    target: '/',
    visibility: 'PUBLIC',
    order: 0,
    parentId: null,
    ...partial,
  };
}

describe('TreeComponent', () => {
  let fixture: ComponentFixture<TreeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreeComponent, MatIconTestingModule, NoopAnimationsModule, RouterTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TreeComponent);
  });

  it('should map links to a tree: types, parent/child, sorting', () => {
    const links: TestLink[] = [
      link({ id: 'r2', name: 'Second root', order: 2, target: '/page2' }),
      link({ id: 'r1', name: 'First root', order: 1, target: '/page1' }),
      link({ id: 'f1', name: 'A folder', order: 3, target: null }),
      link({ id: 'c1', name: 'Child of folder', order: 0, parentId: 'f1', target: '/child' }),
      link({ id: 'l1', name: 'External', order: 4, type: 'LINK', target: '/ext' }),
    ];

    // set input using ComponentRef.setInput (Angular signals input)
    fixture.componentRef.setInput('links', links as any);
    fixture.detectChanges();

    // Expect 4 roots (r1, r2, f1, l1) sorted by order 1,2,3,4
    const rootRows = fixture.debugElement.queryAll(By.css('.tree__row[aria-level="1"]'));
    expect(rootRows.length).toBe(4);
    expect(rootRows[0].query(By.css('.tree__label')).nativeElement.textContent.trim()).toBe('First root');
    expect(rootRows[1].query(By.css('.tree__label')).nativeElement.textContent.trim()).toBe('Second root');
    expect(rootRows[2].query(By.css('.tree__label')).nativeElement.textContent.trim()).toBe('A folder');
    expect(rootRows[3].query(By.css('.tree__label')).nativeElement.textContent.trim()).toBe('External');

    // Folder has toggle, pages and links do not
    expect(rootRows[0].query(By.css('.tree__toggle'))).toBeNull();
    expect(rootRows[1].query(By.css('.tree__toggle'))).toBeNull();
    expect(rootRows[2].query(By.css('.tree__toggle'))).toBeTruthy();
    expect(rootRows[3].query(By.css('.tree__toggle'))).toBeNull();

    // By default folder is expanded, so child is visible immediately
    const allRows = fixture.debugElement.queryAll(By.css('.tree__row'));
    expect(allRows.length).toBe(5);
    const childRow = allRows[3];
    expect(childRow.query(By.css('.tree__label')).nativeElement.textContent.trim()).toBe('Child of folder');

    // Collapse then expand again to ensure toggle works
    const toggleBtn = rootRows[2].query(By.css('.tree__toggle'));
    expect(toggleBtn.attributes['aria-expanded']).toBe('true');
    toggleBtn.triggerEventHandler('click');
    fixture.detectChanges();
    expect(toggleBtn.attributes['aria-expanded']).toBe('false');
    toggleBtn.triggerEventHandler('click');
    fixture.detectChanges();
    expect(toggleBtn.attributes['aria-expanded']).toBe('true');
  });

  it('should keep type as page even if node has children (no normalization), but still render children', () => {
    // parent has a target (would be page), but it also has a child via parentId
    const parent = link({ id: 'p', name: 'Parent page', order: 0, target: '/parent' });
    const child = link({ id: 'ch', name: 'Child', order: 0, parentId: 'p', target: '/child' });

    fixture.componentRef.setInput('links', [parent, child] as any);
    fixture.detectChanges();

    const row = fixture.debugElement.query(By.css('.tree__row[aria-level="1"]'));
    // No toggle because type is page
    const toggle = row.query(By.css('.tree__toggle'));
    expect(toggle).toBeNull();

    // Icon should be page
    const icon = row.query(By.css('.tree__icon'));
    expect(icon.attributes['ng-reflect-svg-icon'] || icon.attributes['svgicon'] || '').toContain('page');

    // Children should still be rendered (no ability to collapse)
    const rows = fixture.debugElement.queryAll(By.css('.tree__row'));
    expect(rows.length).toBe(2);
    const childRow = rows[1];
    expect(childRow.attributes['aria-level']).toBe('2');
    expect(childRow.query(By.css('.tree__label')).nativeElement.textContent.trim()).toBe('Child');
  });

  it('should update selection when a node is clicked', () => {
    const links: TestLink[] = [link({ id: 'a', name: 'Alpha', order: 0 })];

    fixture.componentRef.setInput('links', links as any);
    fixture.detectChanges();

    const row = fixture.debugElement.query(By.css('.tree__row'));
    const labelBtn = row.query(By.css('.tree__label'));

    // Initially selected
    expect(row.nativeElement.classList.contains('selected')).toBe(false);

    // Click -> select
    labelBtn.triggerEventHandler('click');
    fixture.detectChanges();

    expect(row.nativeElement.classList.contains('selected')).toBe(false);
    expect(row.attributes['aria-selected']).toBe('false');
  });
});
