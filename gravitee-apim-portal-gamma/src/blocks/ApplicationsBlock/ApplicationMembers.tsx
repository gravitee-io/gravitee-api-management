/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { useMemo, useState } from 'react';

import type { ApplicationMember } from '../../features/editor/entities/member';
import { PRIMARY_OWNER_ROLE } from '../../features/editor/entities/member';
import { getMembersForApplication } from '../../features/editor/services/applications.mock';

import styles from './ApplicationsBlock.module.scss';

interface ApplicationMembersProps {
    readonly applicationId: string;
}

export function ApplicationMembers({ applicationId }: ApplicationMembersProps) {
    const [search, setSearch] = useState('');
    const [members, setMembers] = useState<ApplicationMember[]>(() => getMembersForApplication(applicationId));
    const [showAddDialog, setShowAddDialog] = useState(false);
    const [newMemberName, setNewMemberName] = useState('');
    const [newMemberEmail, setNewMemberEmail] = useState('');
    const [newMemberRole, setNewMemberRole] = useState('USER');

    const filteredMembers = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return members;
        return members.filter(
            member =>
                member.displayName.toLowerCase().includes(query) ||
                (member.email?.toLowerCase().includes(query) ?? false),
        );
    }, [members, search]);

    const handleAddMember = () => {
        if (!newMemberName.trim()) return;
        const member: ApplicationMember = {
            id: crypto.randomUUID(),
            displayName: newMemberName.trim(),
            email: newMemberEmail.trim() || undefined,
            role: newMemberRole,
            created_at: new Date().toISOString(),
        };
        setMembers(prev => [...prev, member]);
        setNewMemberName('');
        setNewMemberEmail('');
        setNewMemberRole('USER');
        setShowAddDialog(false);
    };

    const handleRemoveMember = (memberId: string) => {
        setMembers(prev => prev.filter(member => member.id !== memberId));
    };

    return (
        <div className={styles.membersPanel}>
            <div className={styles.membersToolbar}>
                <label className={styles.searchField}>
                    <span className={styles.srOnly}>Search members</span>
                    <input
                        className={styles.formInput}
                        value={search}
                        onChange={event => setSearch(event.target.value)}
                        placeholder="Search members…"
                    />
                </label>
                <button type="button" className={styles.primaryBtn} onClick={() => setShowAddDialog(true)}>
                    Add member
                </button>
            </div>

            <h3 className={styles.membersTitle}>Members ({members.length})</h3>

            {filteredMembers.length === 0 ? (
                <p className={styles.emptyMessage}>No members found.</p>
            ) : (
                <div className={styles.tableWrapper}>
                    <table className={styles.table}>
                        <thead>
                            <tr>
                                <th scope="col">Name</th>
                                <th scope="col">Role</th>
                                <th scope="col">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredMembers.map(member => (
                                <tr key={member.id}>
                                    <td>
                                        <div className={styles.memberName}>{member.displayName}</div>
                                        {member.email && <div className={styles.memberEmail}>{member.email}</div>}
                                    </td>
                                    <td>
                                        <span className={styles.badge}>{member.role}</span>
                                    </td>
                                    <td>
                                        {member.role !== PRIMARY_OWNER_ROLE && (
                                            <button
                                                type="button"
                                                className={styles.linkBtn}
                                                onClick={() => handleRemoveMember(member.id)}
                                            >
                                                Remove
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {showAddDialog && (
                <div className={styles.dialogOverlay} role="presentation" onClick={() => setShowAddDialog(false)}>
                    <div
                        className={styles.dialog}
                        role="dialog"
                        aria-labelledby="add-member-title"
                        onClick={event => event.stopPropagation()}
                    >
                        <h3 id="add-member-title" className={styles.dialogTitle}>Add member</h3>
                        <label className={styles.formField}>
                            <span className={styles.formLabel}>Name *</span>
                            <input
                                className={styles.formInput}
                                value={newMemberName}
                                onChange={event => setNewMemberName(event.target.value)}
                            />
                        </label>
                        <label className={styles.formField}>
                            <span className={styles.formLabel}>Email</span>
                            <input
                                className={styles.formInput}
                                value={newMemberEmail}
                                onChange={event => setNewMemberEmail(event.target.value)}
                            />
                        </label>
                        <label className={styles.formField}>
                            <span className={styles.formLabel}>Role</span>
                            <select
                                className={styles.formInput}
                                value={newMemberRole}
                                onChange={event => setNewMemberRole(event.target.value)}
                            >
                                <option value="USER">User</option>
                                <option value="ADMIN">Admin</option>
                            </select>
                        </label>
                        <div className={styles.dialogActions}>
                            <button type="button" className={styles.secondaryBtn} onClick={() => setShowAddDialog(false)}>
                                Cancel
                            </button>
                            <button
                                type="button"
                                className={styles.primaryBtn}
                                disabled={!newMemberName.trim()}
                                onClick={handleAddMember}
                            >
                                Add
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
