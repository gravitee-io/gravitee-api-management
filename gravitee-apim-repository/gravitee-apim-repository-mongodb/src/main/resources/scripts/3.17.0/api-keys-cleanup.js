/**
 * This script should be run before upgrading in order to avoid any issue
 * linked to https://github.com/spring-projects/spring-data-mongodb/issues/2350$
 * while running the ApiKeySubscriptionUpgrader
 */

// Override this variable if you use prefix
const prefix = '';

function unsetUndefined(doc) {
    return Object.keys(doc).some((k, i) => {
        if (doc[k] === undefined) {
            print('found an undefined field on API key', doc._id, k, 'will be unset');
            delete doc[k];
            return true;
        }
    });
}

db.getCollection(`${prefix}keys`).find({}).forEach(doc => {
    const changed = unsetUndefined(doc);
    if (changed) {
        db.getCollection(`${prefix}keys`).save(doc);
    }
});
