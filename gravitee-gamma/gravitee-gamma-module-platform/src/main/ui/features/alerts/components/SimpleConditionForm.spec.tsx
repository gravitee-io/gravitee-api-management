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

import { SimpleConditionForm } from './SimpleConditionForm';
import { API_METRICS } from '../constants/alertConstants';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

describe('SimpleConditionForm', () => {
    it('does not accept a negative threshold (classic min=1)', () => {
        const onChange = jest.fn();
        render(
            <SimpleConditionForm
                condition={{ type: 'THRESHOLD', property: 'response.response_time', operator: 'GT', threshold: 10 }}
                metrics={API_METRICS}
                onChange={onChange}
            />,
        );

        const input = screen.getByPlaceholderText('e.g. 500') as HTMLInputElement;
        expect(input.min).toBe('1');

        fireEvent.change(input, { target: { value: '-8' } });

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ threshold: 10 }));
    });
});
