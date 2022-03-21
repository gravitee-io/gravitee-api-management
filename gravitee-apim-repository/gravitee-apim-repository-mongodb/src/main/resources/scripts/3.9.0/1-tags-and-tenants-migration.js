print('Migrate tags, tenants and entrypoints to reference DEFAULT organization');
// Override this variable if you use prefix
const prefix = '';

const tags = db.getCollection(`${prefix}tags`);

tags.find({referenceId: {$exists: false}}).forEach((tag) => {
        tag.referenceId = 'DEFAULT';
        tag.referenceType = 'ORGANIZATION';
        tags.save(tag);
    }
);

const tenants = db.getCollection(`${prefix}tenants`);

tenants.find({referenceId: {$exists: false}}).forEach((tenant) => {
        tenant.referenceId = 'DEFAULT';
        tenant.referenceType = 'ORGANIZATION';
        tenants.save(tenant);
    }
);

const entrypoints = db.getCollection(`${prefix}entrypoints`);

entrypoints.find({}).forEach(entrypoint => {
    db.getCollection(`${prefix}environments`).find({'_id': entrypoint.environmentId}).forEach(env => {
        entrypoint.referenceId = env.organizationId;
        entrypoint.referenceType = 'ORGANIZATION';
        delete entrypoint.environmentId;
        entrypoints.save(entrypoint);
    });

})
