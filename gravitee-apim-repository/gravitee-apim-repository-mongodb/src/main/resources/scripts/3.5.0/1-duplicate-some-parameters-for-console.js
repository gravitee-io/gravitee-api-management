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
        const consoleParameter = {
            _id: {
                key: consoleParameterKey,
                referenceId: 'DEFAULT',
                referenceType: 'ORGANIZATION',
            },
            value: parameter.value
        }
        db.parameters.save(consoleParameter);
        print('Console parameter created: ' + consoleParameterKey);
    }

    let portalParameter = {};
    if (duplicatedKeysForConsole.includes(parameter._id) || parameter._id === 'authentication.forceLogin.enabled') {
        const portalParameterKey = parameter._id.startsWith('portal') ? parameter._id : 'portal.' + parameter._id;
        portalParameter = {
            _id: {
                key: portalParameterKey,
                referenceId: parameter.referenceId,
                referenceType: parameter.referenceType,
            },
            value: parameter.value
        }
    } else {
        portalParameter = {
            _id: {
                key: parameter._id,
                referenceId: parameter.referenceId,
                referenceType: parameter.referenceType,
            },
            value: parameter.value
        }
    }
    db.parameters.save(portalParameter);
    db.parameters.remove(parameter);

    print('Portal parameter key updated: ' + portalParameter._id.key);
});
