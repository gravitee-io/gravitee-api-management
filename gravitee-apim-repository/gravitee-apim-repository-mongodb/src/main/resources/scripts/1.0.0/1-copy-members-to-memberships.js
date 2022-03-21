db.apis.find().forEach(
    function(api) {
        for(var i = 0; i < api.members.length; i++) {
            var member = api.members[i];
            db.memberships.insert({
                _id: {
                    userId: member.user.$id, 
                    referenceId: api._id, 
                    referenceType: "API"
                },
                _class: "io.gravitee.repository.mongodb.management.internal.model.MembershipMongo",
                type: member.type,
                createdAt: member.createdAt,
                updatedAt: member.updatedAt
            });
        }
    }
);

db.applications.find().forEach(
    function(application) {
        for(var i = 0; i < application.members.length; i++) {
            var member = application.members[i];
            db.memberships.insert({
                _id: {
                    userId: member.user.$id, 
                    referenceId: application._id, 
                    referenceType: "APPLICATION"
                },
                _class: "io.gravitee.repository.mongodb.management.internal.model.MembershipMongo",
                type: member.type,
                createdAt: member.createdAt,
                updatedAt: member.updatedAt
            });
        }
    }
);