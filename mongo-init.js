db.createUser({user: "tanafaso-db-username", pwd: "tanafaso-db-password", roles: [ "readWrite", "dbAdmin" ]});
db.grantRolesToUser('tanafaso-db-username', [{role: "readWrite", db: 'tanafaso' }]);
