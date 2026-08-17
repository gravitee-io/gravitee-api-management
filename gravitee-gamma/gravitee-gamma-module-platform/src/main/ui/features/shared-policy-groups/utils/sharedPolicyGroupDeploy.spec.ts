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

import { canShowDeployActions, isDeployDisabled, isUndeployDisabled } from './sharedPolicyGroupDeploy';

describe('sharedPolicyGroupDeploy', () => {
    describe('canShowDeployActions', () => {
        it('is true when the user can update and origin is not Kubernetes', () => {
            expect(canShowDeployActions({ originContext: { origin: 'MANAGEMENT' } }, true)).toBe(true);
            expect(canShowDeployActions({}, true)).toBe(true);
        });

        it('is false without update permission', () => {
            expect(canShowDeployActions({ originContext: { origin: 'MANAGEMENT' } }, false)).toBe(false);
        });

        it('is false for Kubernetes-origin groups', () => {
            expect(canShowDeployActions({ originContext: { origin: 'KUBERNETES' } }, true)).toBe(false);
        });
    });

    describe('isDeployDisabled', () => {
        it('is true when already DEPLOYED', () => {
            expect(isDeployDisabled('DEPLOYED')).toBe(true);
        });

        it('is true when there are unsaved studio changes', () => {
            expect(isDeployDisabled('UNDEPLOYED', true)).toBe(true);
            expect(isDeployDisabled('PENDING', true)).toBe(true);
        });

        it('is false for UNDEPLOYED or PENDING without unsaved changes', () => {
            expect(isDeployDisabled('UNDEPLOYED')).toBe(false);
            expect(isDeployDisabled('PENDING')).toBe(false);
            expect(isDeployDisabled(undefined)).toBe(false);
        });
    });

    describe('isUndeployDisabled', () => {
        it('is true when already UNDEPLOYED', () => {
            expect(isUndeployDisabled('UNDEPLOYED')).toBe(true);
        });

        it('is false for DEPLOYED or PENDING', () => {
            expect(isUndeployDisabled('DEPLOYED')).toBe(false);
            expect(isUndeployDisabled('PENDING')).toBe(false);
            expect(isUndeployDisabled(undefined)).toBe(false);
        });
    });
});
