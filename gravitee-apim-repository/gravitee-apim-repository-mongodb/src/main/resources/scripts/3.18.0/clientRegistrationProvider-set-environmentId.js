// Override this variable if you use prefix
const prefix = "";

print(`Add 'environmentId' columns in 'client_registration_providers' table`);

const clientRegistrationProviders = db.getCollection(`${prefix}client_registration_providers`);
clientRegistrationProviders.updateMany({}, { $set: { environmentId: "DEFAULT" } });

print(`Create new indexes in 'client_registration_providers' table`);

clientRegistrationProviders.createIndex({ environmentId: 1 }, { name: "e1" });
clientRegistrationProviders.reIndex();
