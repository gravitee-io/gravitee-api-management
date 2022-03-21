print('Add new permissions to the default REVIEWER role');
db.roles.find({ scope:'API', name:'REVIEWER'}).forEach(role => {
    let newPermissions = role.permissions.filter( p => p < 3000 || p > 3015);
    newPermissions.push(3014);
    role.permissions = newPermissions;
    db.roles.save(role);
});
