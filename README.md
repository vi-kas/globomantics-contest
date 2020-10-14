
## globomantics-contest
-----------------------

This project contains codebase for different modules from Pluralsight course - **Scala Asynchronous Programming**. The course is divided in multiple modules, for each module, the corresponding code is kept in branch called module-x. E.g. *module-3*

To run project specific to a particular module go to respective branch module-3  and use sbt to run the service.
You may use any REST Client e.g. POSTMAN to test the service endpoints.

E.g. To create a new User, you need **userService** running.
You may give sbt command `sbt userService/run` to run **User Service**. Afterwards, you may send a POST request to http://localhost:8080/user with below request JSON (Content-type: application/json).
```json
{
   "id": "313c78ec-1eb8-4410-b284-f75079b15739",
   "name": "Alex Foo",
   "username": "alex_foo",
   "password": "simple_text",
   "email": "alex_foo@mail.com"
}
```
Above request should give you a 201 Created response, if everything goes fine.

Note: The project also uses **Docker** for running postgres database. So to follow along, ensure Docker is installed on your machine and running.

Once Docker is running:

1. Create a directory for docker volume e.g. On Mac/Linux: mkdir -p $HOME/docker/volumes/postgres
2. Run the postgres image docker run --rm --name pg-docker -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 -v $HOME/docker/volumes/postgres:/var/lib/postgresql/data postgres

Ensure if it's running: `docker ps`

The command should show something like:
```bash
7bc71fbb33e0 postgres "docker-entrypoint.s…" 14 minutes ago Up 14 minutes 0.0.0.0:5432->5432/tcp pg-docker
```

Configure postgres by following below steps:
1. Enter inside interactive bash shell on the container to use postgres `docker exec -it pg-docker /bin/bash`
2. Open psql for localhost `psql -h localhost -U postgres -d postgres`
Above command says Open psql server running on localhost under user postgres and connect to database named postgres

3. Create a database, a user, and provide permissions on database to the user created.
> CREATE DATABASE "pgcontestdb" WITH ENCODING 'UTF8';
> CREATE USER "trustworthy" WITH PASSWORD 'trust';
> GRANT ALL ON DATABASE "pgcontestdb" TO trustworthy;

This should set postgres with desired configuration. Above configuration(i.e. databaseName, user and password) is required in project's `resources/application.conf` file:
```
conferencedb = {
  connectionPool = "HikariCP"
  dataSourceClass = "org.postgresql.ds.PGSimpleDataSource"
  properties = {
     serverName = "localhost"
     portNumber = "5432"
     databaseName = "pgcontestdb"
     user = "trustworthy"
     password = "trust"
     url = "jdbc:postgresql://localhost:5432/pgcontestdb"
  }
  numThreads = 10
}
```

To Switch to newly created database:
```
> \c pgcontestdb trustworthy
```

To create the table:
```sql
CREATE TABLE public.users_t (id UUID, name VARCHAR ( 50 ), username VARCHAR ( 50 ) UNIQUE NOT NULL, password VARCHAR ( 50 ), email VARCHAR ( 255 ) UNIQUE NOT NULL);
CREATE TABLE public.contests_t (id UUID, title VARCHAR ( 50 ), durationInMinutes DOUBLE PRECISION);
```