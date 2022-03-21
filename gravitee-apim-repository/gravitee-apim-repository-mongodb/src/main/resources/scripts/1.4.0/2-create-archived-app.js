var subAppIds = [];
var allAppIds = [];

db.subscriptions.find({},{application: 1}).forEach(
  function(sub) {
    subAppIds.push(sub.application);
  }
);

db.applications.find({}, {_id: 1}).forEach(
  function(app) {
    allAppIds.push(app._id);
  }
);

now = new Date();
subAppIds.forEach(
  function (appId) {
    if (allAppIds.indexOf(appId) === -1) {
      db.applications.insertOne(
        {
          _id: appId,
          _class : "io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo",
          name: "Archived App",
          description: "Archived application created in version 1.4.0",
          status: "ARCHIVED",
          createdAt: now,
          updatedAt: now
        }
      );

      /*
        Applications must have a PRIMARY_OWNER member. 
        Fill the `userId` attribute with the one you choose.
        Choose the Administration by defaut.
       */
      db.memberships.insertOne(
        {
          _id : {
            userId : "Fill with the user ID",
            referenceId : appId,
            referenceType : "APPLICATION"
          },
          _class : "io.gravitee.repository.mongodb.management.internal.model.MembershipMongo",
          type : "PRIMARY_OWNER",
          createdAt : now,
          updatedAt : now
        }
      );
    }
  }
);
