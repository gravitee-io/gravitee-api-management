const prefix = "";

// "apis" collection
db.getCollection(`${prefix}apis`).dropIndexes();
db.getCollection(`${prefix}apis`).createIndex( { "visibility" : 1 }, { "name": "v1" } );
db.getCollection(`${prefix}apis`).createIndex( { "group" : 1 }, { "name": "g1" } );
db.getCollection(`${prefix}apis`).createIndex( { "name" : 1 }, { "name": "n1" } );
db.getCollection(`${prefix}apis`).reIndex();

// "applications" collection
db.getCollection(`${prefix}applications`).dropIndexes();
db.getCollection(`${prefix}applications`).createIndex( { "group" : 1 }, { "name": "g1" } );
db.getCollection(`${prefix}applications`).createIndex( { "name" : 1 }, { "name": "n1" } );
db.getCollection(`${prefix}applications`).createIndex( { "status" : 1 }, { "name": "s1" } );
db.getCollection(`${prefix}applications`).reIndex();

// "events" collection
db.getCollection(`${prefix}events`).dropIndexes();
db.getCollection(`${prefix}events`).createIndex( { "type" : 1 }, { "name": "t1" } );
db.getCollection(`${prefix}events`).createIndex( { "updatedAt" : 1 }, { "name": "u1" } );
db.getCollection(`${prefix}events`).createIndex( { "updatedAt" : -1, "_id" : -1 }, { "name": "u1i1" } );
db.getCollection(`${prefix}events`).createIndex( { "properties.api_id" : 1 }, { "name": "pa1" } );
db.getCollection(`${prefix}events`).createIndex( { "properties.api_id":1, "type":1}, { "name": "pa1t1" } );
db.getCollection(`${prefix}events`).createIndex( { "properties.api_id":1, "updatedAt" : 1.0}, { "name": "pa1u1" } );
db.getCollection(`${prefix}events`).createIndex( { "type" : 1, "updatedAt" : 1}, { "name": "t1u1" } );
db.getCollection(`${prefix}events`).reIndex();

// "plans" collection
db.getCollection(`${prefix}plans`).dropIndexes();
db.getCollection(`${prefix}plans`).createIndex( { "api" : 1 }, { "name": "a1" } );
db.getCollection(`${prefix}plans`).reIndex();

// "subscriptions" collection
db.getCollection(`${prefix}subscriptions`).dropIndexes();
db.getCollection(`${prefix}subscriptions`).createIndex( { "plan" : 1 }, { "name": "p1" } );
db.getCollection(`${prefix}subscriptions`).createIndex( { "application" : 1 }, { "name": "a1" } );
db.getCollection(`${prefix}subscriptions`).reIndex();

// "keys" collection
db.getCollection(`${prefix}keys`).dropIndexes();
db.getCollection(`${prefix}keys`).createIndex( { "plan" : 1 }, { "name": "p1" } );
db.getCollection(`${prefix}keys`).createIndex( { "application" : 1 }, { "name": "a1" } );
db.getCollection(`${prefix}keys`).createIndex( { "updatedAt" : 1 }, { "name": "u1" } );
db.getCollection(`${prefix}keys`).createIndex( { "revoked" : 1 }, { "name": "r1" });
db.getCollection(`${prefix}keys`).createIndex( { "plan" : 1 , "revoked" : 1, "updatedAt" : 1 }, { "name": "p1r1u1" } );
db.getCollection(`${prefix}keys`).createIndex( { "key" : 1 }, { "name": "k1" } );
db.getCollection(`${prefix}keys`).createIndex( { "key" : 1, "api" : 1 }, { "name": "k1a1" } );
db.getCollection(`${prefix}keys`).reIndex();

// "pages" collection
db.getCollection(`${prefix}pages`).dropIndexes();
db.getCollection(`${prefix}pages`).createIndex( { "api" : 1 }, { "name": "a1" } );
db.getCollection(`${prefix}pages`).createIndex( { "useAutoFetch" : 1 }, { "name": "u1" } );
db.getCollection(`${prefix}pages`).reIndex();

// "memberships" collection
db.getCollection(`${prefix}memberships`).dropIndexes();
db.getCollection(`${prefix}memberships`).createIndex( { "memberId" : 1 }, { "name": "mi1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "member" : 1 }, { "name": "m1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceId" : 1 }, { "name": "ri1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceType" : 1 }, { "name": "rt1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceId":1, "referenceType":1 }, { "name": "ri1rt1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "referenceId":1, "referenceType":1, "roleId":1 }, { "name": "ri1rt1ro1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "roleId" : 1 }, { "name": "ro1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1 }, { "name": "mi1mt1rt1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1, "roleId":1 }, { "name": "mi1mt1rt1ro1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1, "referenceId":1 }, { "name": "mi1mt1rt1ri1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1, "referenceType":1, "referenceId":1, "roleId":1 }, { "name": "mi1mt1rt1ri1ro1" } );
db.getCollection(`${prefix}memberships`).createIndex( { "memberId":1, "memberType":1 }, { "name": "mt1" } );
db.getCollection(`${prefix}memberships`).reIndex();

// "roles" collection
db.getCollection(`${prefix}roles`).dropIndexes();
db.getCollection(`${prefix}roles`).createIndex( {"scope": 1 }, { "name": "s1" } );
db.getCollection(`${prefix}roles`).reIndex();

// "audits" collection
db.getCollection(`${prefix}audits`).dropIndexes();
db.getCollection(`${prefix}audits`).createIndex( { "referenceType": 1, "referenceId": 1 }, { "name": "rt1ri1" } );
db.getCollection(`${prefix}audits`).createIndex( { "createdAt": 1 }, { "name": "c1" } );
db.getCollection(`${prefix}audits`).createIndex({ organizationId: 1 }, { name: "o1" });
db.getCollection(`${prefix}audits`).createIndex({ organizationId: 1, environmentId: 1 }, { name: "o1e1" });
db.getCollection(`${prefix}audits`).reIndex();

// "rating" collection
db.getCollection(`${prefix}rating`).dropIndexes();
db.getCollection(`${prefix}rating`).createIndex( { "api" : 1 }, { "name": "a1" } );
db.getCollection(`${prefix}rating`).reIndex();

// "ratingAnswers" collection
db.getCollection(`${prefix}ratingAnswers`).dropIndexes();
db.getCollection(`${prefix}ratingAnswers`).createIndex( { "rating" : 1 }, { "name": "r1" } );

// "portalnotifications" collection
db.getCollection(`${prefix}portalnotifications`).dropIndexes();
db.getCollection(`${prefix}portalnotifications`).createIndex( { "user" : 1 }, { "name": "u1" } );
db.getCollection(`${prefix}portalnotifications`).reIndex();

// "portalnotificationconfigs" collection
db.getCollection(`${prefix}portalnotificationconfigs`).dropIndexes();
db.getCollection(`${prefix}portalnotificationconfigs`).createIndex( {"_id.user":1, "_id.referenceId":1, "_id.referenceType":1}, { unique: true, name: "iu1iri1irt1" } );
db.getCollection(`${prefix}portalnotificationconfigs`).createIndex( {"_id.referenceId":1, "_id.referenceType":1, "hooks":1}, { "name": "iri1irt1" });
db.getCollection(`${prefix}portalnotificationconfigs`).reIndex();

// "genericnotificationconfigs" collection
db.getCollection(`${prefix}genericnotificationconfigs`).dropIndexes();
db.getCollection(`${prefix}genericnotificationconfigs`).createIndex( {"referenceId":1, "referenceType":1, "hooks":1}, { "name": "ri1rt1h1" });
db.getCollection(`${prefix}genericnotificationconfigs`).createIndex( {"referenceId":1, "referenceType":1}, { "name": "ri1rt1" });
db.getCollection(`${prefix}genericnotificationconfigs`).reIndex();

// "alert triggers" collection
db.getCollection(`${prefix}alert_triggers`).dropIndexes();
db.getCollection(`${prefix}alert_triggers`).createIndex( { "referenceType": 1, "referenceId": 1 }, { "name": "rt1ri1" } );
db.getCollection(`${prefix}alert_triggers`).reIndex();

// "alert events" collection
db.getCollection(`${prefix}alert_events`).dropIndexes();
db.getCollection(`${prefix}alert_events`).createIndex( { "alert": 1 }, { "name": "a1" } );
db.getCollection(`${prefix}alert_events`).createIndex( { "createdAt": 1 }, { "name": "c1" } );
db.getCollection(`${prefix}alert_events`).reIndex();

// "customUserFields" collection
db.getCollection(`${prefix}custom_user_fields`).dropIndexes();
db.getCollection(`${prefix}custom_user_fields`).createIndex( {"_id.referenceId":1, "_id.referenceType":1}, { unique: false, name: "iri1irt1" } );
db.getCollection(`${prefix}custom_user_fields`).reIndex();

// "clientRegistrationProviders" collection
db.getCollection(`${prefix}client_registration_providers`).dropIndexes();
clientRegistrationProviders.createIndex({ environmentId: 1 }, { name: "e1" });
db.getCollection(`${prefix}client_registration_providers`).reIndex();
