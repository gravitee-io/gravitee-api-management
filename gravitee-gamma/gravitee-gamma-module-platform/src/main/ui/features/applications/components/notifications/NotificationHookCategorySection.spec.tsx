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
import { fireEvent, render, screen } from '@testing-library/react';

import { NotificationHookCategorySection } from './NotificationHookCategorySection';
import {
    CERTIFICATE_CLOSE_TO_EXPIRY_HOOK,
    CERTIFICATE_EXPIRY_HOOK,
    SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK,
} from '../../utils/applicationNotificationHooks';

describe('NotificationHookCategorySection', () => {
    it('shows a days input when a close-to-expiry event is selected', () => {
        const onCloseToExpiryDaysChange = jest.fn();
        render(
            <NotificationHookCategorySection
                category={{ name: 'SUBSCRIPTION', hooks: [SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK] }}
                selectedHooks={new Set(['SUBSCRIPTION_CLOSE_TO_EXPIRY'])}
                groupHookIds={new Set()}
                disabled={false}
                closeToExpiryDaysByHookId={{ SUBSCRIPTION_CLOSE_TO_EXPIRY: 30 }}
                onToggle={jest.fn()}
                onCloseToExpiryDaysChange={onCloseToExpiryDaysChange}
            />,
        );

        fireEvent.change(screen.getByLabelText(/days before subscription expiry/i), { target: { value: '14' } });
        expect(onCloseToExpiryDaysChange).toHaveBeenCalledWith('SUBSCRIPTION_CLOSE_TO_EXPIRY', 14);
    });

    it('renders Certificate expiry events without a Support header', () => {
        render(
            <NotificationHookCategorySection
                category={{ name: 'CERTIFICATE', hooks: [CERTIFICATE_EXPIRY_HOOK, CERTIFICATE_CLOSE_TO_EXPIRY_HOOK] }}
                selectedHooks={new Set(['CERTIFICATE_CLOSE_TO_EXPIRY'])}
                groupHookIds={new Set()}
                disabled={false}
                closeToExpiryDaysByHookId={{ CERTIFICATE_CLOSE_TO_EXPIRY: 7 }}
                onToggle={jest.fn()}
                onCloseToExpiryDaysChange={jest.fn()}
            />,
        );

        expect(screen.getByText('CERTIFICATE')).not.toBeNull();
        expect(screen.getByText('Certificate Expiry')).not.toBeNull();
        expect(screen.getByText('Certificate close to expiry')).not.toBeNull();
        expect(screen.getByLabelText(/days before certificate expiry/i)).not.toBeNull();
        expect(screen.queryByText('SUPPORT')).toBeNull();
    });
});
