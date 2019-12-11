# Blob Router

![](https://github.com/hmcts/blob-router-service/workflows/CI/badge.svg)
[![codecov](https://codecov.io/gh/hmcts/blob-router-service/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/blob-router-service)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c99af8bcd53947deb32e8f0a7c500676)](https://www.codacy.com/manual/HMCTS/blob-router-service)

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/blob-router-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `8584` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8584/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

## API (gateway)

Blob Router uses an (Azure API Management) API to protect its SAS token dispensing endpoint.
The API allows only HTTPS requests with approved client certificates and valid subscription keys to reach
the service.

Azure API Management is based on public swagger specs.
As part of creating API in there documentation had to be [published](.github/workflows/swagger.yml).
The full url to documentation can be found [here](infrastructure/api-mgmt.tf).

If SAS dispensing endpoint has changed in some incompatible way which causes amended specs - the management needs to be notified.
This means tiny alteration in [terraform file](infrastructure/api-mgmt.tf).

In case any new endpoint needs to be included - same treatment must be applied.

### Calling the API

In order to talk to the SAS dispensing endpoint through the API, you need to have the following pieces
of information:

- a certificate whose thumbprint is known to the API (has to be added to the list of allowed thumbprints in `var.allowed_client_certificate_thumbprints` terraform variable)
- a valid subscription key
- name of an existing client service (e.g. `test`)

### Preparing client certificate

First, generate client private key, a certificate for that key and import both into a key store:

```
# generate private key
openssl genrsa 2048 > private.pem

# generate certificate
openssl req -x509 -new -key private.pem -out cert.pem -days 365

# create the key store
# when asked for password, provide one
openssl pkcs12 -export -in cert.pem -inkey private.pem -out cert.pfx -noiter -nomaciter
```

Next, calculate the thumbprint of your certificate:

```
openssl x509 -noout -fingerprint -inform pem -in cert.pem | sed -e s/://g
```

Finally, add this thumbprint to `allowed_client_certificate_thumbprints` terraform variable for the target environment (e.g. in `aat.tfvars` file). Your definition may look similar to this:

```
allowed_client_certificate_thumbprints = ["2FC66765E63BB2436F0F9E4F59E951A6D1D20D43"]
```

Once you're run the deployment, the API will recognise your certificate.

### Retrieving subscription key

You can get your subscription key for the API using Azure Portal. In order to do this, perform the following steps:

- Search for the right API Management service instance (`core-api-mgmt-{environment}`) and navigate to its page
- From the API Management service page, navigate to Developer portal (`Developer portal` link at the top bar)
- In developer portal navigate to `Products` tab and click on `blob-router`
- Navigate to `Subscriptions` which holds the list of them. At least 1 (default) should be present
- Click on the `...` at the right of selected subscription and choose `Show/hide keys`. This will toggle the keys. You will need to provide one of the Primary/Secondary value in your request to the API.

### Getting the token through the API

You can call the API using the following curl command (assuming your current directory contains the private key
and certificate you've created earlier):

```
curl -v --key private.pem --cert cert.pem https://core-api-mgmt-{environment}.azure-api.net/reform-scan/token/{service name} -H "Ocp-Apim-Subscription-Key:{subscription key}"
```

You should get a response with status 200 and a token in the body.

### API tests

Jenkins (pipeline) runs the API gateway tests by executing `apiGateway` gradle task. This happens because
there's a call to `enableApiGatewayTest()` in [Jenkins file](/Jenkinsfile_CNP). API tests
are located in [apiGatewayTest](/src/apiGatewayTest/java/uk/gov/hmcts/reform/blobrouter) directory.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details


