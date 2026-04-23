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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserCellComponent, UserCellVM } from './user-cell.component';
import { UserCellHarness } from './user-cell.harness';

describe('UserCellComponent', () => {
  let fixture: ComponentFixture<UserCellComponent>;
  let component: UserCellComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserCellComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UserCellComponent);
    component = fixture.componentInstance;
  });

  function render(user: UserCellVM, isCurrentUser = false): void {
    fixture.componentRef.setInput('user', user);
    fixture.componentRef.setInput('isCurrentUser', isCurrentUser);
    fixture.detectChanges();
  }

  async function getHarness(): Promise<UserCellHarness> {
    return await TestbedHarnessEnvironment.harnessForFixture(fixture, UserCellHarness);
  }

  it('should render displayName as primary label', async () => {
    render({ displayName: 'Alice Smith', initials: 'AS' });
    const harness = await getHarness();
    expect(await harness.getPrimaryText()).toBe('Alice Smith');
  });

  it('should render email as caption text when present', async () => {
    render({ displayName: 'Alice', email: 'alice@example.com', initials: 'A' });
    const harness = await getHarness();
    expect(await harness.getCaptionText()).toBe('alice@example.com');
  });

  it('should not render caption text when email is missing', async () => {
    render({ displayName: 'Alice', initials: 'A' });
    const harness = await getHarness();
    expect(await harness.hasCaption()).toBe(false);
  });

  it('should show initials when avatarUrl is missing', async () => {
    render({ displayName: 'Alice Smith', initials: 'AS' });
    const harness = await getHarness();
    expect(await harness.getInitialsText()).toBe('AS');
    expect(await harness.getAvatar()).toBeNull();
  });

  it('should show avatar image when avatarUrl is present', async () => {
    render({ displayName: 'Alice', initials: 'A', avatarUrl: '/api/avatar/alice' });
    const harness = await getHarness();
    const src = await harness.getAvatarSrc();
    expect(src).toContain('/api/avatar/alice');
    expect(await harness.getInitialsText()).toBeNull();
  });

  it('should fall back to initials on image load error', async () => {
    render({ displayName: 'Alice', initials: 'A', avatarUrl: '/api/avatar/broken' });
    const harness = await getHarness();
    await harness.triggerAvatarError();
    fixture.detectChanges();

    expect(await harness.getAvatar()).toBeNull();
    expect(await harness.getInitialsText()).toBe('A');
  });

  it('should reset fallback state when avatar URL changes', async () => {
    render({ displayName: 'Alice', initials: 'A', avatarUrl: '/api/avatar/broken' });
    let harness = await getHarness();
    await harness.triggerAvatarError();
    fixture.detectChanges();
    harness = await getHarness();
    expect(await harness.getAvatar()).toBeNull();

    render({ displayName: 'Alice', initials: 'A', avatarUrl: '/api/avatar/fresh' });
    harness = await getHarness();
    expect(await harness.getAvatar()).not.toBeNull();
  });

  it('should render (you) label when isCurrentUser is true', async () => {
    render({ displayName: 'Me', initials: 'M' }, true);
    const harness = await getHarness();
    expect(await harness.hasYouBadge()).toBe(true);
  });

  it('should not render (you) label when isCurrentUser is false', async () => {
    render({ displayName: 'Other', initials: 'O' }, false);
    const harness = await getHarness();
    expect(await harness.hasYouBadge()).toBe(false);
  });

  it('should expose component for direct use', () => {
    expect(component).toBeTruthy();
  });
});
