print('Add a prefix to all collections');

const collections = [
    'apiqualityrules',
    'applications',
    'keys',
    'identity_provider_activations',
    'users',
    'tickets',
    'genericnotificationconfigs',
    'workflows',
    'environments',
    'invitations',
    'client_registration_providers',
    'page_revisions',
    'ratingAnswers',
    'apis',
    'rating',
    'themes',
    'metadata',
    'alert_triggers',
    'parameters',
    'dashboards',
    'events',
    'identity_providers',
    'audits',
    'categories',
    'tenants',
    'portalnotifications',
    'custom_user_fields',
    'alert_events',
    'roles',
    'entrypoints',
    'metadatas',
    'memberships',
    'dictionaries',
    'qualityrules',
    'pages',
    'groups',
    'portalnotificationconfigs',
    'installation',
    'notificationTemplates',
    'commands',
    'tokens',
    'apiheaders',
    'plans',
    'tags',
    'subscriptions',
    'organizations',
];

try {
    // Use your prefix here
    const prefix = "";
    const rateLimitPrefix = "";
    collections.forEach(collectionName => {
        db.getCollection(collectionName).renameCollection(`${prefix}${collectionName}`);
    })

    db.ratelimit.renameCollection(`${rateLimitPrefix}ratelimit`);
} catch(e) {
    print(`Error while renaming collection.\nError: ${e}`);
}
