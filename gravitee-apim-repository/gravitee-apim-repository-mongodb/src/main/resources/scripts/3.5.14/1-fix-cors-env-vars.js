const duplicatedKeysForConsole = [
    'portal.http.cors.allow-origin',
    'portal.http.cors.allow-headers',
    'portal.http.cors.allow-methods',
    'portal.http.cors.exposed-headers',
    'portal.http.cors.max-age',
    'console.http.cors.allow-origin',
    'console.http.cors.allow-headers',
    'console.http.cors.allow-methods',
    'console.http.cors.exposed-headers',
    'console.http.cors.max-age'
];

print('Rename portal/console CORS parameters');
db.parameters.find({ '_id.referenceId': { $exists: true }}).forEach(parameter => {
    if (duplicatedKeysForConsole.includes(parameter._id.key)) {
        const parameterKey = parameter._id.key.replace('portal.http.cors', 'http.api.portal.cors')
            .replace('console.http.cors', 'http.api.management.cors');
        const parameterUpdated = {
            _id: {
                key: parameterKey,
                referenceId: parameter._id.referenceId,
                referenceType: parameter._id.referenceType,
            },
            value: parameter.value
        }
        db.parameters.insertOne(parameterUpdated);
        print('Parameter created: ' + parameterKey);
        db.parameters.remove(parameter);
    }
});
