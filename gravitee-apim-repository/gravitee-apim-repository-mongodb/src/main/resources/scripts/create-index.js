const prefix = "";

// "apis" collection
db.getCollection(`${prefix}apis`).dropIndexes();
db.getCollection(`${prefix}apis`).createIndex( { "visibility" : 1 } );
db.getCollection(`${prefix}apis`).createIndex( { "group" : 1 } );
db.getCollection(`${prefix}apis`).reIndex();

// "applications" collection
db.getCollection(`${prefix}applications`).dropIndexes();
db.getCollection(`${prefix}applications`).createIndex( { "group" : 1 } );
db.getCollection(`${prefix}applications`).createIndex( { "name" : 1 } );
db.getCollection(`${prefix}applications`).createIndex( { "status" : 1 } );
db.getCollection(`${prefix}applications`).reIndex();

// "events" collection
db.getCollection(`${prefix}events`).dropIndexes();
db.getCollection(`${prefix}events`).createIndex( { "type" : 1 } );
db.getCollection(`${prefix}events`).createIndex( { "updatedAt" : 1 } );
db.getCollection(`${prefix}events`).createIndex( { "properties.api_id" : 1 } );
db.getCollection(`${prefix}events`).createIndex( { "properties.api_id":1, "type":1} );
db.getCollection(`${prefix}events`).reIndex();

// "plans" collection
db.getCollection(`${prefix}plans`).dropIndexes();
db.getCollection(`${prefix}plans`).createIndex( { "api" : 1 } );
db.getCollection(`${prefix}plans`).reIndex();

// "subscriptions" collection
db.getCollection(`${prefix}subscriptions`).dropIndexes();
db.getCollection(`${prefix}subscriptions`).createIndex( { "plan" : 1 } );
db.getCollection(`${prefix}subscriptions`).createIndex( { "application" : 1 } );
db.getCollection(`${prefix}subscriptions`).reIndex();

// "keys" collection
db.getCollection(`${prefix}keys`).dropIndexes();
db.getCollection(`${prefix}keys`).createIndex( { "plan" : 1 } );
db.getCollection(`${prefix}keys`).createIndex( { "application" : 1 } );
db.getCollection(`${prefix}keys`).createIndex( { "updatedAt" : 1 } );
db.getCollection(`${prefix}keys`).createIndex( { "revoked" : 1 } );
db.getCollection(`${prefix}keys`).createIndex( { "plan" : 1 , "revoked" : 1, "updatedAt" : 1 } );
db.getCollection(`${prefix}keys`).createIndex( { "key" : 1 } );
db.getCollection(`${prefix}keys`).createIndex( { "key" : 1, "api" : 1 } );
db.getCollection(`${prefix}keys`).reIndex();

// "pages" collection
db.getCollection(`${prefix}pages`).dropIndexes();
db.getCollection(`${prefix}pages`).createIndex( { "api" : 1 } );
db.getCollection(`${prefix}pages`).createIndex( { "useAutoFetch" : 1 } );
db.getCollection(`${prefix}pages`).reIndex();

// "memberships" collection
db.getCollection(`${prefix}memberships`).dropIndexes();
db.getCollection(`${prefix}memberships`).createIndex( { "memberId" : 1 } );
db.getCollection(`${prefix}memberships`).createIndex( { "member" : 1 } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceId" : 1 } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceType" : 1 } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceId":1, "referenceType":1 } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceId":1, "referenceType":1, "roleId":1 } );
db.getCollection(`${prefix}memberships`).createIndex( { "roleId" : 1 } );
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1 });
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1, "roleId":1 });
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1, "referenceId":1 });
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1, "referenceId":1, "roleId":1 });
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1 });
db.getCollection(`${prefix}memberships`).reIndex();

// "roles" collection
db.getCollection(`${prefix}roles`).dropIndexes();
db.getCollection(`${prefix}roles`).createIndex( {"scope": 1 } );
db.getCollection(`${prefix}roles`).reIndex();

// "audits" collection
db.getCollection(`${prefix}audits`).dropIndexes();
db.getCollection(`${prefix}audits`).createIndex( { "referenceType": 1, "referenceId": 1 } );
db.getCollection(`${prefix}audits`).createIndex( { "createdAt": 1 } );
db.getCollection(`${prefix}audits`).reIndex();

// "rating" collection
db.getCollection(`${prefix}rating`).dropIndexes();
db.getCollection(`${prefix}rating`).createIndex( { "api" : 1 } );
db.getCollection(`${prefix}rating`).reIndex();

// "ratingAnswers" collection
db.getCollection(`${prefix}ratingAnswers`).dropIndexes();
db.getCollection(`${prefix}ratingAnswers`).createIndex( { "rating" : 1 } );

// "portalnotifications" collection
db.getCollection(`${prefix}portalnotifications`).dropIndexes();
db.getCollection(`${prefix}portalnotifications`).createIndex( { "user" : 1 } );
db.getCollection(`${prefix}portalnotifications`).reIndex();

// "portalnotificationconfigs" collection
db.getCollection(`${prefix}portalnotificationconfigs`).dropIndexes();
db.getCollection(`${prefix}portalnotificationconfigs`).createIndex( {"_id.user":1, "_id.referenceId":1, "_id.referenceType":1}, { unique: true } );
db.getCollection(`${prefix}portalnotificationconfigs`).createIndex( {"_id.referenceId":1, "_id.referenceType":1, "hooks":1});
db.getCollection(`${prefix}portalnotificationconfigs`).reIndex();

// "genericnotificationconfigs" collection
db.getCollection(`${prefix}genericnotificationconfigs`).dropIndexes();
db.getCollection(`${prefix}genericnotificationconfigs`).createIndex( {"referenceId":1, "referenceType":1, "hooks":1});
db.getCollection(`${prefix}genericnotificationconfigs`).createIndex( {"referenceId":1, "referenceType":1});
db.getCollection(`${prefix}genericnotificationconfigs`).reIndex();

// "alert triggers" collection
db.getCollection(`${prefix}alert_triggers`).dropIndexes();
db.getCollection(`${prefix}alert_triggers`).createIndex( { "referenceType": 1, "referenceId": 1 } );
db.getCollection(`${prefix}alert_triggers`).reIndex();

// "alert events" collection
db.getCollection(`${prefix}alert_events`).dropIndexes();
db.getCollection(`${prefix}alert_events`).createIndex( { "alert": 1 } );
db.getCollection(`${prefix}alert_events`).createIndex( { "createdAt": 1 } );
db.getCollection(`${prefix}alert_events`).reIndex();

// "customUserFields" collection
db.getCollection(`${prefix}custom_user_fields`).dropIndexes();
db.getCollection(`${prefix}custom_user_fields`).createIndex( {"_id.referenceId":1, "_id.referenceType":1}, { unique: false } );
db.getCollection(`${prefix}custom_user_fields`).reIndex();
