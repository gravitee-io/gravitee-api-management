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
import { SecurityPlanFields } from './SecurityPlanFields';
import { useApiCreation } from '../../store/apiCreationStore';
import { isTcpForm } from '../../utils/protocol';

export function SecurityStep() {
    const { state } = useApiCreation();
    const isTcp = isTcpForm(state.form);

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h2 className="text-base font-semibold">Security & Plans</h2>
                <p className="text-sm text-muted-foreground">
                    {isTcp
                        ? 'TCP proxies only support a Keyless plan. Consumers connect without HTTP authentication.'
                        : 'Choose how consumers authenticate and configure the access plan.'}
                </p>
            </div>

            <SecurityPlanFields showAuthSelector={!isTcp} />
        </div>
    );
}
