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
import userEvent from '@testing-library/user-event';
import { useState } from 'react';

import { RulesetFormatCards } from './RulesetFormatCards';
import { GRAVITEE_API_DEFINITION, type DefinitionFormatSelection, type RulesetFormat } from '../types/rulesets';

function Harness() {
    const [definitionFormat, setDefinitionFormat] = useState<DefinitionFormatSelection | ''>('');
    const [graviteeApiFormat, setGraviteeApiFormat] = useState<RulesetFormat | ''>('');
    return (
        <RulesetFormatCards
            definitionFormat={definitionFormat}
            graviteeApiFormat={graviteeApiFormat}
            onDefinitionFormatChange={setDefinitionFormat}
            onGraviteeApiFormatChange={setGraviteeApiFormat}
        />
    );
}

describe('RulesetFormatCards', () => {
    it('shows OpenAPI, AsyncAPI and Gravitee API, and reveals nested Gravitee formats after Gravitee API', async () => {
        const user = userEvent.setup();
        render(<Harness />);

        expect(screen.getByTestId('definition-format-OPENAPI')).not.toBeNull();
        expect(screen.getByTestId('definition-format-ASYNCAPI')).not.toBeNull();
        expect(screen.queryByTestId('gravitee-api-format-selection')).toBeNull();

        await user.click(screen.getByTestId(`definition-format-${GRAVITEE_API_DEFINITION}`));
        expect(screen.getByTestId('gravitee-api-format-selection')).not.toBeNull();
        expect(screen.getByText('Gravitee Proxy API')).not.toBeNull();
        expect(screen.getByText('Native Kafka')).not.toBeNull();
        expect(screen.getByText('Gravitee V2 API')).not.toBeNull();
    });
});
