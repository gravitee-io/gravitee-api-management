print('In users collection, \'referenceId\' and \'referenceType\' are replaced with \'organizationId\'');
db.users.find({}).forEach(user => {
    if (!user.organizationId) {
        const organizationId = user.referenceId;
        db.users.updateOne(
            { '_id': user._id },
            {
                $set: { 'organizationId': organizationId },
                $unset: { 'referenceId': '', 'referenceType': '' }
            }
        );
    }
});

print('In identity_providers collection, \'referenceId\' and \'referenceType\' are replaced with \'organizationId\'');
db.identity_providers.find({}).forEach(idp => {
    if (!idp.organizationId) {
        const organizationId = idp.referenceId;
        db.identity_providers.updateOne(
            { '_id': idp._id },
            {
                $set: { 'organizationId': organizationId },
                $unset: { 'referenceId': '', 'referenceType': '' }
            }
        );
    }
});
