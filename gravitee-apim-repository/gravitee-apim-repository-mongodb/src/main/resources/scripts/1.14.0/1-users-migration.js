function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}

print('Users migration');

db.users.find().forEach(
    function(user) {
        if (user.username === undefined) {
            var oldUserId = user._id;
            print('Updating user: ' + oldUserId);

            user._id = guid();
            user.username = oldUserId;

            // insert the user, using the new _id
            db.users.insert(user)

            // remove the user with the old _id
            db.users.remove({'_id': oldUserId})

            print('Updating memberships for user: ' + oldUserId);
            db.memberships.find({'_id.userId': oldUserId}).forEach(
                function(membership) {
		    if (membership !== undefined) {
                        membership._id.userId = user._id;

                        // insert the membership, using the new _id
                        print('Create new membership for new user ID: ' + membership._id.userId);
                        db.memberships.insert(membership)

                        // remove the membership with the old _id
                        print('Removing memberships for old user ID: ' + oldUserId);
                        db.memberships.remove({'_id.userId': oldUserId, '_id.referenceType': membership._id.referenceType, '_id.referenceId': membership._id.referenceId})
		    }
                }
            );
        } else {
            print('User: ' + user._id + ' already have a username [' + user.username+ '], skipping');
	}
    }
)

print('Audits migration');

db.audits.find({ 'username': { $exists: true } }).forEach(
    function(audit) {
        if (audit.username !== undefined) {
            var user = db.users.findOne({'username': audit.username});
            if (user === undefined || user === null) {
                print('User: ' + audit.username+ ' does not exist, skipping');
            } else {
                db.audits.updateOne(
                    { _id: audit._id },
                    { $set:{ 'user': user._id }, $unset: { 'username': "" }},
                    { upsert: false });
            }
        }
    }
)

print('System audit migration');
db.audits.find( { 'username': { $exists: true } } ).forEach(
    function(audit) {
        db.audits.updateOne(
            { _id: audit._id },
            { $set:{ 'user': audit.username }, $unset: { 'username': "" }},
            { upsert: false });
    }
)

print('Rating migration');

db.rating.find().forEach(
    function(rating) {
        var user = db.users.findOne({'username': rating.user});
        if (user === undefined || user === null) {
            print('User: ' + rating.user + ' does not exist, skipping');
        } else {
            db.rating.updateOne(
                { _id: rating._id },
                { $set:{ 'user': user._id }},
                { upsert: false });
        }
    }
)

print('Events migration');

db.events.find( { 'properties.username': { $exists: true } } ).forEach(
    function(event) {
        var user = db.users.findOne( {'username': event.properties.username});
        if (user === undefined || user === null) {
            print('User: ' + event.properties.username + ' does not exist. Use current username as the user system reference');
            db.events.updateOne(
                { _id: event._id },
                { $set:{ 'properties.user': event.properties.username }, $unset: { 'properties.username': "" }},
                { upsert: false });
        } else {
            db.events.updateOne(
                { _id: event._id },
                { $set:{ 'properties.user': user._id }, $unset: { 'properties.username': "" }},
                { upsert: false });
        }
    }
)

print('Subscriptions migration');
db.subscriptions.find().forEach(
    function(subscription) {
        if (subscription.subscribedBy !== undefined) {
            var subscriber = db.users.findOne( {'username': subscription.subscribedBy});
            if (subscriber === undefined || subscriber === null) {
                print('User: ' + subscription.subscribedBy + ' does not exist. Skipping');
            } else {
                db.subscriptions.updateOne(
                    { _id: subscription._id },
                    { $set:{ 'subscribedBy': subscriber._id } },
                    { upsert: false });
            }
        }

        if (subscription.processedBy !== undefined) {
            var processedBy = db.users.findOne( {'username': subscription.processedBy});
            if (processedBy === undefined || processedBy === null) {
                print('User: ' + subscription.processedBy + ' does not exist. Skipping');
            } else {
                db.subscriptions.updateOne(
                    { _id: subscription._id },
                    { $set:{ 'processedBy': processedBy._id } },
                    { upsert: false });
            }
        }
    }
)
