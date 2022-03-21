print('Apiheaders migration - add environmentId field');
db.apiheaders.find().forEach(
    function(apiHeader) {
        if (!apiHeader.environmentId) {
            let existingApiHeaderCursor = db.apiheaders.find({'_id': apiHeader._id, environmentId: 'DEFAULT'});
            if(!existingApiHeaderCursor.hasNext()) {
                apiHeader.environmentId = 'DEFAULT';
                db.apiheaders.replaceOne({ _id: apiHeader._id }, apiHeader);
            }
        }
    }
);

print('Apis migration - add environmentId field');
db.apis.update({}, {$set: {'environmentId': 'DEFAULT'}}, false, true);

print('Applications migration - add environmentId field');
db.applications.update({}, {$set: {'environmentId': 'DEFAULT'}}, false, true);

print('Commands migration - add environmentId field');
db.commands.update({}, {$set: {'environmentId': 'DEFAULT'}}, false, true);

print('Dictionaries migration - add environmentId field');
db.dictionaries.update({}, {$set: {'environmentId': 'DEFAULT'}}, false, true);

print('Events migration - add environmentId field');
db.events.update({}, {$set: {'environmentId': 'DEFAULT'}}, false, true);

print('Entrypoints migration - add environmentId field');
db.entrypoints.update({}, {$set: {'environmentId': 'DEFAULT'}}, false, true);

print('Views migration - add environmentId field');
db.views.find().forEach(
    function(view) {
        if (!view.environmentId) {
            let existingViewCursor = db.views.find({'_id': view._id, environmentId: 'DEFAULT'});
            if(!existingViewCursor.hasNext()) {
                view.environmentId = 'DEFAULT';
                db.views.replaceOne({ _id: view._id }, view);
            }
        }
    }
);

print('IdentityProviders migration - add referenceId and referenceType field');
db.identity_providers.find().forEach(
    function(idp) {
        if (!idp.referenceId) {
            let existingIDPCursor = db.identity_providers.find({'_id': idp._id, referenceId: 'DEFAULT', referenceType: 'ENVIRONMENT'});
            if(!existingIDPCursor.hasNext()) {
                idp.referenceId = 'DEFAULT';
                idp.referenceType = 'ENVIRONMENT';
                db.identity_providers.replaceOne({ _id: idp._id }, idp);
            }
        }
    }
);
print('Parameters migration - add referenceId and referenceType field');
db.parameters.update({}, {$set: {referenceId: 'DEFAULT', referenceType: 'ENVIRONMENT'}}, false, true);

print('Users migration - add referenceId and referenceType field');
db.users.update({}, {$set: {referenceId: 'DEFAULT', referenceType: 'ORGANIZATION'}}, false, true);

print('Pages migration - add referenceId and referenceType field');
db.pages.find().forEach(
    function(page) {
        var referenceId ='';
        var referenceType = '';

        if(!page.referenceId) {
            if(page.api) {
                referenceId = page.api;
                referenceType = 'API';
            } else {
                referenceId = 'DEFAULT';
                referenceType = 'ENVIRONMENT';
            }
            
            db.pages.updateOne(
                { '_id': page._id },
                {
                    $set: { 'referenceId': referenceId, 'referenceType': referenceType },
                    $unset: { 'api': ''}
                }
            );
        }
    }
);

print('Rating migration - add referenceId and referenceType field');
db.rating.find().forEach(
    function(rating) {
        var referenceId ='';
        var referenceType = '';
        
        if(!rating.referenceId) {
            if(rating.api) {
                referenceId = rating.api;
                referenceType = 'API';
            } else {
                referenceId = 'DEFAULT';
                referenceType = 'ENVIRONMENT';
            }
            
            db.rating.updateOne(
                { '_id': rating._id },
                {
                    $set: { 'referenceId': referenceId, 'referenceType': referenceType },
                    $unset: { 'api': ''}
                }
            );
        }
    }
);
