# ccpay-bulkscanning-app
[![Build Status](https://travis-ci.org/hmcts/ccpay-bulkscanning-app.svg?branch=master)](https://travis-ci.org/hmcts/ccpay-bulkscanning-app)
# Bulk scanning payments API

## Notes x

Since Spring Boot 2.1 bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is now enabled by default in the project. Please refrain from using JUnit4 and use the next generation

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application on IntelliJ

1. Add spring_profiles_active=local in the BulkscanningApiApplication configuration settings
2. Enable annotation processing under settings/compiler in development environment
3. Setup a postgre database called 'bspayment' and create login group for it. Enable 'can login' under login group/privileges
4. Edit the application-local.yaml file, add the following details if not present already:
   url: jdbc:postgresql://localhost:5432/bspayment
   username: DBusername
   password: DBpassword
5. Ensure that bar-idam-mock is already running and run the application:
   https://github.com/hmcts/bar-idam-mock

6. Open  http://localhost:8081/swagger-ui.html to check if the api is running.

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/ccpay-bulkscanning-app` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `1234` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:1234/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```
### Running the application using docker-compose

Add the below variables and its values to .env file in below format
Key = Value

1. POSTGRES_USER -  bspayment
2. POSTGRES_PASSWORD  - bspayment
3. POSTGRES_DB - bspayment
4. OPENID_SPRING_DATASOURCE_PASSWORD - openidm
5. IDAM_SPI_FORGEROCK_AM_PASSWORD - Pa55word11
6. IDAM_SPI_FORGEROCK_IDM_PASSWORD - openidm-admin
7. IDAM_SPI_FORGEROCK_IDM_PIN_DEFAULTPASSWORD - BlaBlaBlackSh33p
8. SECURITY_OAUTH2_CLIENT_CLIENTSECRET - password

The docker compose file picks up the values from .env file when the below command is executed:
```bash
docker-compose up
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

#### Environment variables

The following environment variables are required:

- `APPINSIGHTS_INSTRUMENTATIONKEY`, app insights key to send telemetry events.
It will be updated.

