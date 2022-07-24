db.createUser(
        {
            user: "tanafaso-db-username",
            pwd: "tanafaso-db-password",
            roles: [
                {
                    role: "readWrite",
                    db: "tanafaso"
                }
            ]
        }
);