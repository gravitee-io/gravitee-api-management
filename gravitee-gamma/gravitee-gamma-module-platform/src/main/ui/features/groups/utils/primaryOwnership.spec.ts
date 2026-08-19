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

import type { MemberRoleSelections } from './memberRoles';
import { analyzeEditOwnershipTransfer, buildEditMembershipPayloads, buildEditOwnershipTransferMessage } from './primaryOwnership';
import type { GroupMember } from '../types/group';

const SELECTIONS: Pick<
    MemberRoleSelections,
    'apiRole' | 'apiProductRole' | 'applicationRole' | 'integrationRole' | 'clusterRole' | 'explorerRole'
> = {
    apiRole: 'USER',
    apiProductRole: '',
    applicationRole: 'USER',
    integrationRole: '',
    clusterRole: '',
    explorerRole: '',
};

const ANNA: GroupMember = { id: 'user-1', displayName: 'Anna Schmidt', roles: { APPLICATION: 'PRIMARY_OWNER', API: 'USER' } };
const RAVI: GroupMember = { id: 'user-2', displayName: 'Ravi Patel', roles: { APPLICATION: 'OWNER', API: 'USER' } };

describe('analyzeEditOwnershipTransfer', () => {
    it('requires a successor when demoting an Application primary owner', () => {
        const transfer = analyzeEditOwnershipTransfer(ANNA, [ANNA, RAVI], { ...SELECTIONS, applicationRole: 'OWNER' });

        expect(transfer.needsSuccessor).toBe(true);
        expect(transfer.downgradeScopes).toEqual(['APPLICATION']);
    });

    it('records the existing Application owner when promoting another member', () => {
        const transfer = analyzeEditOwnershipTransfer(RAVI, [ANNA, RAVI], { ...SELECTIONS, applicationRole: 'PRIMARY_OWNER' });

        expect(transfer.existingOwners.APPLICATION).toEqual(ANNA);
        expect(transfer.needsSuccessor).toBe(false);
    });

    it('does not treat Explorer as a transferable primary-owner scope', () => {
        const explorerOwner: GroupMember = { id: 'user-1', displayName: 'Anna Schmidt', roles: { EXPLORER: 'PRIMARY_OWNER' } };
        const transfer = analyzeEditOwnershipTransfer(explorerOwner, [explorerOwner, RAVI], {
            ...SELECTIONS,
            explorerRole: 'USER',
        });

        expect(transfer.needsSuccessor).toBe(false);
        expect(transfer.downgradeScopes).toEqual([]);
    });
});

describe('buildEditOwnershipTransferMessage', () => {
    it('describes an Application primary-owner downgrade', () => {
        const selections = { ...SELECTIONS, applicationRole: 'OWNER' };
        const transfer = analyzeEditOwnershipTransfer(ANNA, [ANNA, RAVI], selections);

        expect(buildEditOwnershipTransferMessage(ANNA, transfer, RAVI, selections)).toBe(
            'Anna Schmidt is the Application primary owner. The Application primary ownership will be transferred to Ravi Patel and Anna Schmidt will be updated as owner.',
        );
    });
});

describe('buildEditMembershipPayloads', () => {
    it('promotes the successor to Application PRIMARY_OWNER and demotes the edited member', () => {
        const selections: MemberRoleSelections = { ...SELECTIONS, applicationRole: 'OWNER' };
        const transfer = analyzeEditOwnershipTransfer(ANNA, [ANNA, RAVI], selections);

        expect(buildEditMembershipPayloads(ANNA, selections, transfer, RAVI)).toEqual([
            {
                id: 'user-2',
                roles: [
                    { scope: 'APPLICATION', name: 'PRIMARY_OWNER' },
                    { scope: 'API', name: 'USER' },
                ],
            },
            {
                id: 'user-1',
                roles: [
                    { scope: 'API', name: 'USER' },
                    { scope: 'APPLICATION', name: 'OWNER' },
                ],
            },
        ]);
    });

    it('demotes the existing Application owner when the edited member is promoted', () => {
        const selections: MemberRoleSelections = { ...SELECTIONS, applicationRole: 'PRIMARY_OWNER' };
        const transfer = analyzeEditOwnershipTransfer(RAVI, [ANNA, RAVI], selections);

        expect(buildEditMembershipPayloads(RAVI, selections, transfer, null)).toEqual([
            {
                id: 'user-1',
                roles: [
                    { scope: 'APPLICATION', name: 'OWNER' },
                    { scope: 'API', name: 'USER' },
                ],
            },
            {
                id: 'user-2',
                roles: [
                    { scope: 'API', name: 'USER' },
                    { scope: 'APPLICATION', name: 'PRIMARY_OWNER' },
                ],
            },
        ]);
    });
});
