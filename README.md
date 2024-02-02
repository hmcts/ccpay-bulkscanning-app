[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Aay_ccpay-bulkscanning-app_master&metric=alert_status)](https://sonarcloud.io/dashboard?id=uk.gov.hmcts.reform%3Aay_ccpay-bulkscanning-app_master)
[![Build Status](https://travis-ci.org/hmcts/spring-boot-template.svg?branch=master)](https://travis-ci.org/hmcts/ccpay-bulkscanning-app)
# Bulk scanning payments API

## Purpose

The purpose of Bulk-Scanning API is to capture payment data from Bulk-scanning component and Exela and move it to pay-hub once all payment data has been received.


## What's inside this application

The api is a working application with a setup. It contains:
 * application
 * setup script to prepare project
 * common plugins and libraries
 * docker setup
 * swagger configuration for api documentation ([see how to publish your api documentation to shared repository](https://github.com/hmcts/reform-api-docs#publish-swagger-docs))
 * code quality tools already set up
 * integration with Travis CI
 * Hystrix circuit breaker enabled
 * MIT license and contribution information
 * Helm chart using chart-java.

The application exposes health endpoint (http://localhost:4550/health) and metrics endpoint
(http://localhost:4550/metrics).

## Plugins

The template contains the following plugins:

  * checkstyle

    https://docs.gradle.org/current/userguide/checkstyle_plugin.html

    Performs code style checks on Java source files using Checkstyle and generates reports from these checks.
    The checks are included in gradle's *check* task (you can run them by executing `./gradlew check` command).

  * pmd

    https://docs.gradle.org/current/userguide/pmd_plugin.html

    Performs static code analysis to finds common programming flaws. Included in gradle `check` task.


  * jacoco

    https://docs.gradle.org/current/userguide/jacoco_plugin.html

    Provides code coverage metrics for Java code via integration with JaCoCo.
    You can create the report by running the following command:

    ```bash
      ./gradlew jacocoTestReport
    ```

    The report will be created in build/reports subdirectory in your project directory.

  * io.spring.dependency-management

    https://github.com/spring-gradle-plugins/dependency-management-plugin

    Provides Maven-like dependency management. Allows you to declare dependency management
    using `dependency 'groupId:artifactId:version'`
    or `dependency group:'group', name:'name', version:version'`.

  * org.springframework.boot

    http://projects.spring.io/spring-boot/

    Reduces the amount of work needed to create a Spring application

  * org.owasp.dependencycheck

    https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html

    Provides monitoring of the project's dependent libraries and creating a report
    of known vulnerable components that are included in the build. To run it
    execute `gradle dependencyCheck` command.

  * com.github.ben-manes.versions

    https://github.com/ben-manes/gradle-versions-plugin

    Provides a task to determine which dependencies have updates. Usage:

    ```bash
      ./gradlew dependencyUpdates -Drevision=release
    ```

## Setup

Located in `./bin/init.sh`. Simply run and follow the explanation how to execute it.

## Notes

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

Create the image  of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/spring-boot-template` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `4550` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4550/health
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

## Hystrix

[Hystrix](https://github.com/Netflix/Hystrix/wiki) is a library that helps you control the interactions
between your application and other services by adding latency tolerance and fault tolerance logic. It does this
by isolating points of access between the services, stopping cascading failures across them,
and providing fallback options. We recommend you to use Hystrix in your application if it calls any services.

### Hystrix circuit breaker

This template API has [Hystrix Circuit Breaker](https://github.com/Netflix/Hystrix/wiki/How-it-Works#circuit-breaker)
already enabled. It monitors and manages all the`@HystrixCommand` or `HystrixObservableCommand` annotated methods
inside `@Component` or `@Service` annotated classes.

### Other

Hystrix offers much more than Circuit Breaker pattern implementation or command monitoring.
Here are some other functionalities it provides:
 * [Separate, per-dependency thread pools](https://github.com/Netflix/Hystrix/wiki/How-it-Works#isolation)
 * [Semaphores](https://github.com/Netflix/Hystrix/wiki/How-it-Works#semaphores), which you can use to limit
 the number of concurrent calls to any given dependency
 * [Request caching](https://github.com/Netflix/Hystrix/wiki/How-it-Works#request-caching), allowing
 different code paths to execute Hystrix Commands without worrying about duplicating work

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

#### Environment variables

The following environment variables are required:

- `APPINSIGHTS_INSTRUMENTATIONKEY`, app insights key to send telemetry events.
It will be updated.

