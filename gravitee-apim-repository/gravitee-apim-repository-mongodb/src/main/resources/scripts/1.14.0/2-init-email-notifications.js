var now = new Date();

function guid() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

function getConfig(membership, user) {
  var hooks = [];
  if ("API" === membership._id.referenceType) {
    hooks = [
      "APIKEY_EXPIRED",
      "APIKEY_REVOKED",
      "SUBSCRIPTION_NEW",
      "SUBSCRIPTION_ACCEPTED",
      "SUBSCRIPTION_CLOSED",
      "SUBSCRIPTION_REJECTED"
    ];
  } else {
    hooks = [
      "SUBSCRIPTION_NEW",
      "SUBSCRIPTION_ACCEPTED",
      "SUBSCRIPTION_CLOSED",
      "SUBSCRIPTION_REJECTED"
    ];
  }
  return {
    _id: guid(),
    _class: 'io.gravitee.repository.mongodb.management.internal.model.GenericNotificationConfigMongo',
    name: "Default Mail Notifications",
    referenceType: membership._id.referenceType,
    referenceId: membership._id.referenceId,
    notifier: "default-email",
    config: user.email,
    hooks: hooks,
    createdAt: now,
    updatedAt: now
  }
}

db.memberships.find({'roles': {$in: ['3:PRIMARY_OWNER', '4:PRIMARY_OWNER']}}).forEach(
  function (membership) {
    var user = db.users.findOne({'_id': membership._id.userId});
    if (user && user.email) {
      print ("Create an email notification for "
        + user.email
        + " and " + membership._id.referenceType
        + " " + membership._id.referenceId);

      db.genericnotificationconfigs.insert(getConfig(membership, user))
    }
  }
);

