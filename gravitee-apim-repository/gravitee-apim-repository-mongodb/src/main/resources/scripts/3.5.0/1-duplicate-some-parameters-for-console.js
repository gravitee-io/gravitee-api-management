const duplicatedKeysForConsole = [
    'authentication.localLogin.enabled',
    'portal.support.enabled',
    'portal.userCreation.enabled',
    'portal.userCreation.automaticValidation.enabled',
    'scheduler.tasks',
    'scheduler.notifications',
    'reCaptcha.enabled',
    'reCaptcha.siteKey',
    'http.cors.allow-origin',
    'http.cors.allow-headers',
    'http.cors.allow-methods',
    'http.cors.exposed-headers',
    'http.cors.max-age'
];

print('In parameters collection duplicate some key for authentication, recaptcha, schedulers and cors, and change the _id to add refId/refType');
db.parameters.find({ referenceId: { $exists: true }}).forEach(parameter => {
    if (duplicatedKeysForConsole.includes(parameter._id)) {
        const consoleParameterKey = parameter._id.startsWith('portal') ? parameter._id.replace('portal', 'console') : 'console.' + parameter._id;
        const consoleId = {
            key: consoleParameterKey,
            referenceId: 'DEFAULT',
            referenceType: 'ORGANIZATION',
        };
        const consoleParameter = { _id: consoleId, value: parameter.value };
        db.parameters.replaceOne({ _id: consoleId }, consoleParameter, { upsert: true });
        print('Console parameter created: ' + consoleParameterKey);
    }

    let portalParameterKey;
    if (duplicatedKeysForConsole.includes(parameter._id) || parameter._id === 'authentication.forceLogin.enabled') {
        portalParameterKey = parameter._id.startsWith('portal') ? parameter._id : 'portal.' + parameter._id;
    } else {
        portalParameterKey = parameter._id;
    }
    const portalId = {
        key: portalParameterKey,
        referenceId: parameter.referenceId,
        referenceType: parameter.referenceType,
    };
    const portalParameter = { _id: portalId, value: parameter.value }
    db.parameters.replaceOne({ _id: portalId }, portalParameter, { upsert: true });
    print('Portal parameter key updated: ' + portalParameter._id.key);

    db.parameters.remove(parameter);
});