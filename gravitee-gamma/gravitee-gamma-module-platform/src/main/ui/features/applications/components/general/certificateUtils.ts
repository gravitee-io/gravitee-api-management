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
import type { ClientCertificate, ClientCertificateStatus } from '../../types/applicationCertificate';

export function certificateStatusLabel(status: ClientCertificateStatus): string {
    switch (status) {
        case 'ACTIVE':
        case 'ACTIVE_WITH_END':
            return 'Active';
        case 'SCHEDULED':
            return 'Scheduled';
        case 'REVOKED':
            return 'Revoked';
        default:
            return status;
    }
}

export function certificateStatusVariant(status: ClientCertificateStatus): 'default' | 'secondary' | 'destructive' | 'outline' {
    if (status === 'REVOKED') return 'destructive';
    if (status === 'SCHEDULED') return 'outline';
    return 'secondary';
}

export function findActiveCertificate(certificates: ClientCertificate[]): ClientCertificate | undefined {
    const sorted = [...certificates].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    return sorted.find(c => c.status === 'ACTIVE') ?? sorted.find(c => c.status === 'ACTIVE_WITH_END');
}

export function readFileAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsText(file);
    });
}

export function isDateInRange(value: Date, min: Date, max?: Date): boolean {
    if (Number.isNaN(value.getTime())) return false;
    if (value.getTime() < min.getTime()) return false;
    if (max && value.getTime() > max.getTime()) return false;
    return true;
}
