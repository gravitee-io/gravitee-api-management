/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

export enum ClientCertificateStatus {
  ACTIVE = 'ACTIVE',
  ACTIVE_WITH_END = 'ACTIVE_WITH_END',
  SCHEDULED = 'SCHEDULED',
  REVOKED = 'REVOKED',
}

export interface ClientCertificate {
  id: string;
  crossId?: string;
  applicationId?: string;
  name: string;
  startsAt?: string;
  endsAt?: string;
  createdAt: string;
  updatedAt?: string;
  certificate?: string;
  certificateExpiration?: string;
  subject?: string;
  issuer?: string;
  fingerprint?: string;
  environmentId?: string;
  status: ClientCertificateStatus;
}

export interface CreateClientCertificate {
  name: string;
  certificate: string;
  startsAt?: string;
  endsAt?: string;
}

export interface UpdateClientCertificate {
  name: string;
  startsAt?: string;
  endsAt?: string;
}
