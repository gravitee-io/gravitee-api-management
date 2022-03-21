
loadMigrationArray = function () {
    let roleMigrationArray = [];
    roleMigrationArray[1] = [];
    roleMigrationArray[2] = [];
    roleMigrationArray[1][1000]={'newScope':6,'newPermission':1000};
    roleMigrationArray[1][1200]={'newScope':6,'newPermission':1200};
    roleMigrationArray[1][1300]={'newScope':6,'newPermission':1300};
    roleMigrationArray[1][1400]={'newScope':6,'newPermission':1400};
    roleMigrationArray[1][1500]={'newScope':6,'newPermission':1500};
    roleMigrationArray[1][1600]={'newScope':7,'newPermission':1200};
    roleMigrationArray[1][1700]={'newScope':6,'newPermission':1700};
    roleMigrationArray[1][1800]={'newScope':6,'newPermission':1800};
    roleMigrationArray[1][1900]={'newScope':6,'newPermission':1900};
    roleMigrationArray[1][2000]={'newScope':6,'newPermission':2000};
    roleMigrationArray[1][2100]={'newScope':7,'newPermission':1000};
    roleMigrationArray[1][2200]={'newScope':6,'newPermission':2200};
    roleMigrationArray[1][2300]={'newScope':6,'newPermission':2300};
    roleMigrationArray[1][2400]={'newScope':6,'newPermission':2400};
    roleMigrationArray[1][2500]={'newScope':6,'newPermission':2500};
    roleMigrationArray[1][2600]={'newScope':6,'newPermission':2600};
    roleMigrationArray[1][2700]={'newScope':6,'newPermission':2700};
    roleMigrationArray[1][2800]={'newScope':6,'newPermission':2800};
    roleMigrationArray[2][1000]={'newScope':6,'newPermission':2900};
    roleMigrationArray[2][1100]={'newScope':6,'newPermission':3000};
    roleMigrationArray[2][1200]={'newScope':6,'newPermission':1700};
    roleMigrationArray[2][1300]={'newScope':6,'newPermission':3100};
    roleMigrationArray[2][1400]={'newScope':6,'newPermission':3200};
    roleMigrationArray[2][1500]={'newScope':6,'newPermission':2600};
    roleMigrationArray[2][1600]={'newScope':6,'newPermission':3300};
    roleMigrationArray[2][1700]={'newScope':6,'newPermission':3400};
    roleMigrationArray[2][1800]={'newScope':6,'newPermission':3500};
    return roleMigrationArray;
}
roleMigrationArray = loadMigrationArray();

hasPermValue = function (permissions, permValue) {
    for (let index = 0; index < permissions.length; index++) {
        var currentPermValue = permissions[index] - (permissions[index] % 100);
        if (currentPermValue === permValue) {
            return true;
        }
    }
    return false;
}

print('Roles migration - scope renaming & new Id & MANAGEMENT and PORTAL replaced by ENVIRONMENT and ORGANIZATION');
db.roles.dropIndexes();
db.roles.find().sort( { '_id.scope': 1 } ).forEach(
    function(role) {
        const oldId = role._id;
        const oldScope = role._id.scope;
        if (oldScope === 1) {
            if (hasPermValue(role.permissions, 1600) || hasPermValue(role.permissions, 2100)) {

                let newRole = Object.assign({}, role);
                newRole._id = 'ORGANIZATION_' + oldId.name;
                newRole.scope = 'ORGANIZATION';
                newRole.name = oldId.name;
                newRole.referenceId = 'DEFAULT';
                newRole.referenceType = 'ORGANIZATION';

                newRole.permissions = [];
                let roleNewPermissions = []
                role.permissions.forEach(
                    function(permission) {
                        let permValue = permission - (permission % 100);
                        if (permValue === 1600) {
                            newRole.permissions.push(1200 + permission % 100);
                        } else if (permValue === 2100) {
                            newRole.permissions.push(1000 + permission % 100);
                        } else {
                            roleNewPermissions.push(permission);
                        }
                    }
                );
                role.permissions = roleNewPermissions;

                let existingRoleCursor = db.roles.find({scope: 'ORGANIZATION', name: oldId.name, referenceId: 'DEFAULT', referenceType: 'ORGANIZATION'});
                if (existingRoleCursor.hasNext()) {
                    db.roles.remove({'_id': existingRoleCursor.next()._id});
                }
                db.roles.insert(newRole);
            }

            let roleNewPermissions = []
            role.permissions.forEach(
                function(permission) {
                    let permValue = permission - (permission % 100);
                    let migData = roleMigrationArray[oldScope][permValue];
                    if (migData) {
                        roleNewPermissions.push(migData.newPermission + permission % 100);
                    }
                }
            );
            role.permissions = roleNewPermissions;

            let portalRoleCursor = db.roles.find({'_id.scope':2,'_id.name':oldId.name});
            if (portalRoleCursor.hasNext()) {
                let portalRole = portalRoleCursor.next();

                portalRole.permissions.forEach(
                    function(permission) {
                        let permValue = permission - (permission % 100);
                        let migData = roleMigrationArray[2][permValue];
                        if (migData) {
                            let newPermission = migData.newPermission + permission % 100;
                            if(!role.permissions.includes(newPermission)) {
                                role.permissions.push(newPermission);
                            }
                        }
                    }
                );
                db.roles.remove({ '_id' : portalRole._id });
            }
            role._id = 'ENVIRONMENT_' + oldId.name;
            role.scope = 'ENVIRONMENT';
            role.name = oldId.name;
            role.referenceId = 'DEFAULT';
            role.referenceType = 'ORGANIZATION';

            db.roles.remove({ '_id' : oldId });

            let existingRoleCursor = db.roles.find({scope: 'ENVIRONMENT', name: oldId.name, referenceId: 'DEFAULT', referenceType: 'ORGANIZATION'});
            if (existingRoleCursor.hasNext()) {
                db.roles.remove({'_id': existingRoleCursor.next()._id});
            }
            db.roles.insert(role);
    } else if (oldScope === 2) {
            // has already been treated and deleted ?
            let portalRoleCursor = db.roles.find({'_id':oldId});
            if (portalRoleCursor.hasNext()) {
                let roleNewPermissions = []

                role.permissions.forEach(
                    function(permission) {
                        let permValue = permission - (permission % 100);
                        let migData = roleMigrationArray[oldScope][permValue];
                        if (migData) {
                            roleNewPermissions.push(migData.newPermission + permission % 100);
                        }
                    }
                );
                role.permissions = roleNewPermissions;

                role._id = 'ENVIRONMENT_' + oldId.name;
                role.scope = 'ENVIRONMENT';
                role.name = oldId.name;
                role.referenceId = 'DEFAULT';
                role.referenceType = 'ORGANIZATION';

                db.roles.remove({ '_id' : oldId });
                let existingRoleCursor = db.roles.find({scope: 'ENVIRONMENT', name: oldId.name, referenceId: 'DEFAULT', referenceType: 'ORGANIZATION'});
                if (existingRoleCursor.hasNext()) {
                    db.roles.remove({'_id': existingRoleCursor.next()._id});
                }
                db.roles.insert(role);

            }
        } else if (oldScope === 3) {
            role._id = 'API_' + oldId.name;
            role.scope = 'API';
            role.name = oldId.name;
            role.referenceId = 'DEFAULT';
            role.referenceType = 'ORGANIZATION';

            db.roles.remove({ '_id' : oldId });
            let existingRoleCursor = db.roles.find({scope: 'API', name: oldId.name, referenceId: 'DEFAULT', referenceType: 'ORGANIZATION'});
            if (existingRoleCursor.hasNext()) {
                db.roles.remove({'_id': existingRoleCursor.next()._id});
            }
            db.roles.insert(role);

        } else if (oldScope === 4) {
            role._id = 'APPLICATION_' + oldId.name;
            role.scope = 'APPLICATION';
            role.name = oldId.name;
            role.referenceId = 'DEFAULT';
            role.referenceType = 'ORGANIZATION';

            db.roles.remove({ '_id' : oldId });
            let existingRoleCursor = db.roles.find({scope: 'APPLICATION', name: oldId.name, referenceId: 'DEFAULT', referenceType: 'ORGANIZATION'});
            if (existingRoleCursor.hasNext()) {
                db.roles.remove({'_id': existingRoleCursor.next()._id});
            }
            db.roles.insert(role);

        } else if (oldScope === 5) {
            role._id = 'GROUP_' + oldId.name;
            role.scope = 'GROUP';
            role.name = oldId.name;
            role.referenceId = 'DEFAULT';
            role.referenceType = 'ORGANIZATION';

            db.roles.remove({ '_id' : oldId });
            let existingRoleCursor = db.roles.find({scope: 'GROUP', name: oldId.name, referenceId: 'DEFAULT', referenceType: 'ORGANIZATION'});
            if (existingRoleCursor.hasNext()) {
                db.roles.remove({'_id': existingRoleCursor.next()._id});
            }
            db.roles.insert(role);

        }
    }
);
db.roles.createIndex( {"id": 1 } );
db.roles.createIndex( {"scope": 1 } );
db.roles.reIndex();

print('Memberships migration - new structure + MANAGEMENT and PORTAL replaced by ENVIRONMENT and ORGANIZATION');
db.memberships.dropIndexes();
db.memberships.find().forEach(
    function(membership) {
        if(!membership.roleId) {
            const oldId = membership._id;

            membership.roles.forEach(role => {
                let roleArray = role.split(':');
                let newRoleId;
                let newReferenceId;
                let newReferenceType;
                if (roleArray[0] == 1) {
                    let newOrganizationRoleId = 'ORGANIZATION_' + roleArray[1];
                    let portalRoleCursor = db.roles.find({'_id':newOrganizationRoleId});
                    if (portalRoleCursor.hasNext()) {
                        let newOrganizationMembership = {
                            '_id': UUID().toString().split('"')[1],
                            'memberId': oldId.userId,
                            'memberType': 'USER',
                            'referenceId': 'DEFAULT',
                            'referenceType': 'ORGANIZATION',
                            'roleId': newOrganizationRoleId,
                            'createdAt': membership.createdAt,
                            'updatedAt': membership.updatedAt
                        };

                        db.memberships.insert(newOrganizationMembership);
                    }

                    newRoleId = 'ENVIRONMENT_' + roleArray[1];
                    newReferenceId = 'DEFAULT';
                    newReferenceType = 'ENVIRONMENT';
                } else if (roleArray[0] == 2) {
                    newRoleId = 'ENVIRONMENT_' + roleArray[1];
                    newReferenceId = 'DEFAULT';
                    newReferenceType = 'ENVIRONMENT';
                } else if (roleArray[0] == 3) {
                    newRoleId = 'API_' + roleArray[1];
                    newReferenceId = oldId.referenceId;
                    newReferenceType = oldId.referenceType;
                } else if (roleArray[0] == 4) {
                    newRoleId = 'APPLICATION_' + roleArray[1];
                    newReferenceId = oldId.referenceId;
                    newReferenceType = oldId.referenceType;
                } else if (roleArray[0] == 5) {
                    newRoleId = 'GROUP_' + roleArray[1];
                    newReferenceId = oldId.referenceId;
                    newReferenceType = oldId.referenceType;
                }

                let newMembership = {
                    '_id': UUID().toString().split('"')[1],
                    'memberId': oldId.userId,
                    'memberType': 'USER',
                    'referenceId': newReferenceId,
                    'referenceType': newReferenceType,
                    'roleId': newRoleId,
                };
                if (membership.createdAt) {
                    newMembership['createdAt'] = membership.createdAt;
                }
                if (membership.updatedAt) {
                    newMembership['updatedAt'] = membership.updatedAt;
                }

                let countMembership = db.memberships.count({
                    'memberId': newMembership['memberId'],
                    'memberType': newMembership['memberType'],
                    'referenceId': newMembership['referenceId'],
                    'referenceType': newMembership['referenceType'],
                    'roleId': newMembership['roleId']
                });

                if (countMembership === 0) {
                    db.memberships.insert(newMembership);
                }
            });
            db.memberships.remove({'_id' : oldId});
        }
    }
);
db.memberships.createIndex( { "id" : 1 }, { unique : true } );
db.memberships.createIndex( { "memberId" : 1 } );
db.memberships.createIndex( { "member" : 1 } );
db.memberships.createIndex( { "referenceId" : 1 } );
db.memberships.createIndex( { "referenceType" : 1 } );
db.memberships.createIndex( { "referenceId":1, "referenceType":1 } );
db.memberships.createIndex( { "referenceId":1, "referenceType":1, "roleId":1 } );
db.memberships.createIndex( { "roleId" : 1 } );
db.memberships.createIndex( { "memberId":1, "memberType":1, "referenceType":1 });
db.memberships.createIndex( { "memberId":1, "memberType":1, "referenceType":1, "roleId":1 });
db.memberships.createIndex( { "memberId":1, "memberType":1, "referenceType":1, "referenceId":1 });
db.memberships.createIndex( { "memberId":1, "memberType":1, "referenceType":1, "referenceId":1, "roleId":1 });
db.memberships.createIndex( { "memberId":1, "memberType":1 });
db.memberships.reIndex();

print('Groups migration - default role are now store in "memberships" collection + add environmentId field');
db.groups.find().forEach(
    function(group) {
        if(group.roles) {
            group.roles.forEach(role => {
                let roleArray = role.split(':');
                let newRoleId;
                let newReferenceType;
                if (roleArray[0] == 3) {
                    newRoleId = 'API_' + roleArray[1];
                    newReferenceType = 'API';
                } else if (roleArray[0] == 4) {
                    newRoleId = 'APPLICATION_' + roleArray[1];
                    newReferenceType = 'APPLICATION';
                }

                let newMembership = {
                    '_id': UUID().toString().split('"')[1],
                    'memberId': group._id,
                    'memberType': 'GROUP',
                    'referenceId': null,
                    'referenceType': newReferenceType,
                    'roleId': newRoleId,
                    'createdAt': new Date(),
                    'updatedAt': new Date()
                };

                db.memberships.insert(newMembership);
            });
        }
        db.groups.updateOne(
            { _id: group._id },
            {
                $set: {'environmentId': 'DEFAULT'},
                $unset: { roles: '' }
            }
        );
    }
);
