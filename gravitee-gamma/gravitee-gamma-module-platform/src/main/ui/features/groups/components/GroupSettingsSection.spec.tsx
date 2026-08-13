/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen } from '@testing-library/react';

import { GroupSettingsSection } from './GroupSettingsSection';
import type { Group } from '../types/group';

const GROUP: Group = { id: 'group-1', name: 'Support Team' };

describe('GroupSettingsSection', () => {
    it('shows placeholders when no roles, limit, or invitation methods are configured', () => {
        render(<GroupSettingsSection group={GROUP} />);

        expect(screen.getByText('Max members').nextElementSibling?.textContent).toBe('Unlimited');
        expect(screen.getByText('Invitation methods').nextElementSibling?.textContent).toBe('None');
        expect(screen.getByText('Default API role').nextElementSibling?.textContent).toBe('—');
    });

    it('shows the configured default roles with a lock indicator for locked scopes', () => {
        render(
            <GroupSettingsSection
                group={{
                    ...GROUP,
                    roles: { API: 'OWNER', API_PRODUCT: 'USER', APPLICATION: 'USER' },
                    lock_api_role: true,
                }}
            />,
        );

        const apiRoleValue = screen.getByText('Default API role').nextElementSibling;
        expect(apiRoleValue?.textContent).toContain('OWNER');
        expect(apiRoleValue?.querySelector('[aria-label="Locked"]')).not.toBeNull();

        const applicationRoleValue = screen.getByText('Default application role').nextElementSibling;
        expect(applicationRoleValue?.querySelector('[aria-label="Locked"]')).toBeNull();
    });

    it('shows the configured max member limit', () => {
        render(<GroupSettingsSection group={{ ...GROUP, max_invitation: 25 }} />);

        expect(screen.getByText('Max members').nextElementSibling?.textContent).toBe('25');
    });

    it('shows badges for each enabled invitation method', () => {
        render(<GroupSettingsSection group={{ ...GROUP, system_invitation: true, email_invitation: true }} />);

        expect(screen.queryByText('User search')).not.toBeNull();
        expect(screen.queryByText('Email invitation')).not.toBeNull();
    });

    it('shows Yes for notify on new members when notifications are not disabled', () => {
        render(<GroupSettingsSection group={{ ...GROUP, disable_membership_notifications: false }} />);

        expect(screen.getByText('Notify on new members').nextElementSibling?.textContent).toBe('Yes');
    });

    it('shows No for notify on new members when notifications are disabled', () => {
        render(<GroupSettingsSection group={{ ...GROUP, disable_membership_notifications: true }} />);

        expect(screen.getByText('Notify on new members').nextElementSibling?.textContent).toBe('No');
    });
});
