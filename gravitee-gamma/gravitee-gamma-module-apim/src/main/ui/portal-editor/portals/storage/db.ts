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
// Re-exported from portal-gamma to ensure a single source of truth for
// IndexedDB schema version, migrations, and store definitions.
// Both apps share the same browser database ('gravitee-portal-gamma').
export {
    DB_NAME,
    DB_VERSION,
    PORTALS_STORE_NAME,
    NAVIGATION_ITEMS_STORE_NAME,
    PAGE_CONTENTS_STORE_NAME,
    APPLICATIONS_STORE_NAME,
    SUBSCRIPTIONS_STORE_NAME,
    THEMES_STORE_NAME,
    PORTAL_TENANTS_STORE_NAME,
    PORTAL_TENANT_MEMBERS_STORE_NAME,
    PORTAL_CONSUMERS_STORE_NAME,
    PORTAL_INVITATIONS_STORE_NAME,
    PORTAL_CATEGORIES_STORE_NAME,
    PORTAL_SUBSCRIPTION_FORMS_STORE_NAME,
    PORTAL_IDENTITY_PROVIDERS_STORE_NAME,
    TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME,
    PORTAL_DOMAINS_STORE_NAME,
    PAGE_TEMPLATES_STORE_NAME,
    upgradeDatabase,
    openDB,
    runTransaction,
    resetDatabaseSchemaState,
} from '@portal-gamma/features/portals/storage/db';
