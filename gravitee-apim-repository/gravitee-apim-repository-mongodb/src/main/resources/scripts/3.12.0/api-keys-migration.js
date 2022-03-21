// Override this variable if you use prefix
const prefix = '';

print(`Add 'key' and 'api' columns in 'keys' table`);

const keys = db.getCollection(`${prefix}keys`);
const subscriptions = db.getCollection(`${prefix}subscriptions`);

keys.find({}).forEach((key) => {
    subscriptions.find({_id: key.subscription}).forEach((subscription) => {
        key.key = key._id;
        key.api = subscription.api;
        keys.save(key);
    });
});

print(`Create new indexes in 'keys' table`);

keys.createIndex( { "key" : 1 } );
keys.createIndex( { "key" : 1, "api" : 1 } );
keys.reIndex();
