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
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { ApiKeyFeedback, ApiKeysListComponent } from './api-keys-list.component';
import { ApiKeysListHarness } from './api-keys-list.harness';
import { SubscriptionDataKeys } from '../../../entities/subscription';
import { AppTestingModule } from '../../../testing/app-testing.module';

describe('ApiKeysListComponent', () => {
  let component: ApiKeysListComponent;
  let fixture: ComponentFixture<ApiKeysListComponent>;
  let harness: ApiKeysListHarness;

  function activeApiKey(key: string): SubscriptionDataKeys {
    return {
      key,
      created_at: '2024-04-17T10:33:29Z',
      application: { id: 'app-id', name: 'app-name' },
    };
  }

  function revokedApiKey(key: string): SubscriptionDataKeys {
    return {
      key,
      created_at: '2024-04-17T10:33:29Z',
      revoked_at: '2024-04-19T10:33:29Z',
      application: { id: 'app-id', name: 'app-name' },
    };
  }

  async function init(params?: {
    apiKeys?: SubscriptionDataKeys[];
    canManageApiKey?: boolean;
    isRevokeDisabled?: boolean;
    feedback?: ApiKeyFeedback;
  }) {
    await TestBed.configureTestingModule({
      imports: [ApiKeysListComponent, AppTestingModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiKeysListComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('apiKeys', params?.apiKeys ?? []);
    fixture.componentRef.setInput('canManageApiKey', params?.canManageApiKey ?? true);
    fixture.componentRef.setInput('isRevokeDisabled', params?.isRevokeDisabled ?? false);
    fixture.componentRef.setInput('feedback', params?.feedback);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiKeysListHarness);
  }

  it('should show empty message when there are no api keys', async () => {
    await init({ apiKeys: [] });

    expect(await harness.isTableShown()).toBeFalsy();
    expect(await harness.getEmptyMessage()).toContain('No API keys available.');
  });

  it('should display api keys with their status icons', async () => {
    await init({ apiKeys: [activeApiKey('active-api-key'), revokedApiKey('revoked-api-key')] });

    expect(await harness.isTableShown()).toBeTruthy();
    expect(await harness.getKeyAt(0)).toEqual('active-api-key');
    expect(await harness.getKeyAt(1)).toEqual('revoked-api-key');
    expect(await harness.getStatusIcons()).toEqual(['gio:check-circled-outline', 'gio:x-circle']);
  });

  it('should show revoke button only for active api keys', async () => {
    await init({ apiKeys: [activeApiKey('active-api-key'), revokedApiKey('revoked-api-key')] });

    const revokeButtons = await harness.getRevokeButtons();
    expect(revokeButtons).toHaveLength(1);
    expect(await (await revokeButtons[0].host()).getAttribute('data-api-key')).toEqual('active-api-key');
  });

  it('should hide revoke buttons when the user cannot manage api keys', async () => {
    await init({ apiKeys: [activeApiKey('active-api-key')], canManageApiKey: false });

    expect(await harness.getRevokeButtons()).toHaveLength(0);
  });

  it('should disable revoke buttons when revocation is disabled', async () => {
    await init({ apiKeys: [activeApiKey('active-api-key')], isRevokeDisabled: true });

    const revokeButtons = await harness.getRevokeButtons();
    expect(revokeButtons).toHaveLength(1);
    expect(await revokeButtons[0].isDisabled()).toBeTruthy();
  });

  it('should emit revokeApiKey when a revoke button is clicked', async () => {
    await init({ apiKeys: [activeApiKey('active-api-key')] });
    const revokeApiKeySpy = jest.spyOn(component.revokeApiKey, 'emit');

    await harness.clickRevoke('active-api-key');

    expect(revokeApiKeySpy).toHaveBeenCalledWith(expect.objectContaining({ key: 'active-api-key' }));
  });

  it('should show success feedback politely', async () => {
    await init({
      apiKeys: [activeApiKey('active-api-key')],
      feedback: { type: 'success', message: 'API key renewed successfully.' },
    });

    expect(await harness.getFeedbackText()).toContain('API key renewed successfully.');
    expect(await harness.getFeedbackAttribute('aria-live')).toEqual('polite');
    expect(await harness.getFeedbackAttribute('role')).toBeNull();
  });

  it('should show error feedback as an alert', async () => {
    await init({
      apiKeys: [activeApiKey('active-api-key')],
      feedback: { type: 'error', message: 'Failed to renew API key.' },
    });

    expect(await harness.getFeedbackText()).toContain('Failed to renew API key.');
    expect(await harness.getFeedbackAttribute('role')).toEqual('alert');
    expect(await harness.getFeedbackAttribute('aria-live')).toBeNull();
  });

  describe('pagination', () => {
    const apiKeys = Array.from({ length: 7 }, (_, index) => activeApiKey(`api-key-${index + 1}`));

    it('should display the first page of api keys by default', async () => {
      await init({ apiKeys });

      expect(await harness.getDisplayedKeys()).toEqual(['api-key-1', 'api-key-2', 'api-key-3', 'api-key-4', 'api-key-5']);
    });

    it('should display the next page of api keys when navigating', async () => {
      await init({ apiKeys });

      const pagination = await harness.getPagination();
      expect(pagination).toBeTruthy();
      await (await pagination!.getNextPageButton()).click();

      expect(await harness.getDisplayedKeys()).toEqual(['api-key-6', 'api-key-7']);
    });

    it('should display all api keys and reset to the first page when increasing the page size', async () => {
      await init({ apiKeys });

      const pagination = await harness.getPagination();
      await (await pagination!.getNextPageButton()).click();
      await pagination!.changePageSize(10);

      expect(await harness.getDisplayedKeys()).toHaveLength(7);
      expect(await (await pagination!.getCurrentPaginationPage()).getText()).toEqual('1');
    });
  });
});
