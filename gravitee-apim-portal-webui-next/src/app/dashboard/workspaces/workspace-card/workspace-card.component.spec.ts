/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { WorkspaceCardComponent } from './workspace-card.component';

describe('WorkspaceCardComponent', () => {
  let fixture: ComponentFixture<WorkspaceCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkspaceCardComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceCardComponent);
    fixture.componentRef.setInput('workspaceId', 'ws-1');
    fixture.componentRef.setInput('title', 'test-local-spf');
    fixture.componentRef.setInput('budget', { amount: 50.05, period: 'WEEK' });
    fixture.detectChanges();
  });

  it('shows the name, a missing description, and the budget', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('test-local-spf');
    expect(text).toContain('No description');
    expect(text).toContain('$50.05 / week');
  });

  it('shows the description when one is set', () => {
    fixture.componentRef.setInput('description', 'A local proxy');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('A local proxy');
  });

  it('emits the workspace id on click and enter', () => {
    const selected: string[] = [];
    fixture.componentInstance.cardSelect.subscribe(id => selected.push(id));

    fixture.nativeElement.querySelector('mat-card').click();
    fixture.nativeElement.querySelector('mat-card').dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(selected).toEqual(['ws-1', 'ws-1']);
  });
});
