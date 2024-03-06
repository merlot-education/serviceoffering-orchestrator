# MERLOT Serviceoffering Orchestrator
The Serviceoffering Orchestrator is a microservice in the MERLOT marketplace
which handles all Service Offering related information.

Internally, this service wraps self-description communication with the [XFSC Federated Catalog](https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service/-/tree/1.0.1?ref_type=tags) and [XFSC Self-Description Wizard](https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-api)
while also augmenting the self-description with a state machine providing visibility levels
and information about associated contracts.

## Development

To start development for the MERLOT marketplace, please refer to [this document](https://github.com/merlot-education/.github/blob/main/Docs/DevEnv.md)
to set up a local WSL development environment of all relevant services.
This is by far the easiest way to get everything up and running locally.

## Structure

```
├── src/main/java/eu/merloteducation/serviceofferingorchestrator
│   ├── auth            # authorization checks
│   ├── config          # configuration-related components
│   ├── controller      # external REST API controllers
│   ├── mappers         # mappers for DTOs
│   ├── models          # internal data models of serviceoffering-related data
│   ├── repositories    # DAOs for accessing the stored data
│   ├── security        # configuration for route-based authentication
│   ├── service         # internal services for processing data from the controller layer
```

REST API related models such as the DTOs can be found at [models-lib](https://github.com/merlot-education/models-lib/tree/main)
which is shared amongst the microservices.

## Dependencies
- A properly set-up keycloak instance (quay.io/keycloak/keycloak:20.0.5)
- [Organisations Orchestrator](https://github.com/merlot-education/organisations-orchestrator)
- [XFSC Federated Catalogue](https://gitlab.eclipse.org/eclipse/xfsc/cat/fc-service/-/tree/1.0.1?ref_type=tags)
- [XFSC Self-Description Wizard](https://gitlab.eclipse.org/eclipse/xfsc/self-description-tooling/sd-creation-wizard-api)
- rabbitmq (rabbitmq:3-management)

## Build

To build this microservice you need to provide a GitHub read-only token in order to be able to fetch maven packages from
GitHub. You can create this token at https://github.com/settings/tokens with at least the scope "read:packages".
Then set up your ~/.m2/settings.xml file as follows:

    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

        <servers>
            <server>
                <id>github</id>
                <username>REPLACEME_GITHUB_USER</username>
                <!-- Public token with `read:packages` scope -->
                <password>REPLACEME_GITHUB_TOKEN</password>
            </server>
        </servers>
    </settings>

Afterward you can build the service with

    mvn clean package

## Run

    export KEYCLOAK_CLIENTSECRET="mysecret"
    java -jar target/serviceoffering-orchestrator-X.Y.Z.jar

The KEYCLOAK_CLIENTSECRET corresponds to the client secret that is configured for the XFSC Federated Catalogue Realm in your Keycloak instance.

Replace the X.Y.Z with the respective version of the service.

## Deploy (Docker)

This microservice can be deployed as part of the full MERLOT docker stack at
[localdeployment](https://github.com/merlot-education/localdeployment).

## Deploy (Helm)
TODO