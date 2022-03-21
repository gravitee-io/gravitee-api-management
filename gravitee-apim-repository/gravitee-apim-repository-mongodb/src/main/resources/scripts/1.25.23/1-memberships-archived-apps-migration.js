print('Archived apps migration');
db.applications.find({status: 'ARCHIVED'}).forEach(
    function (application) {
        db.memberships.find({"_id.referenceType": "APPLICATION", "_id.referenceId": application._id}).forEach(
            function (membership) {
                db.users.find({"_id": membership._id.userId}).forEach(
                    function (user) {
                        print("Clean application memberships for the archived app '" + application.name + "' and user '" + user.sourceId + "'");
                        db.memberships.deleteOne(membership);
                    })
            })
    });

print('Deleted API\'s Memberships migration');
db.memberships.find({'_id.referenceType': 'API'}).forEach(
    function (membership) {
        const existingApiCursor = db.apis.find({"_id": membership._id.referenceId})
        if (!existingApiCursor.hasNext()) {
            print("Clean API memberships for the deleted API '" + membership._id.referenceId + "' and user '" + membership._id.userId + "'");
            db.memberships.deleteOne(membership);
        }
    });
