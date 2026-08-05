# Keycloak FK API User Federation

Keycloak user storage provider for allowing user federation via the FK(Fælleskommunalt) Api.

To make Keycloak recognize the user storage provider the src/main/resources/META-INF folder and its content is required.

You will need to set the password for the keystore in client.properties. This needs to be done before a build. You also need to have the correct keys.

For the truststore you can find them here: https://digitaliseringskataloget.dk/teknik/certifikater.

For the keystore you will need a cert with a private key that is the same as have been set in https://admin.serviceplatformen.dk/

The Test file is just for testing and is not used anywhere.

For local test make sure to comment `<exclude>keys/**</exclude>` in pom.xml

Also make sure to update client.properties.
`org.apache.wss4j.crypto.merlin.truststore.file` should be set to `keys/truststore.production.jks`

You also need to set the passwords in there

## Build

Requirements are Maven (verified 3.6.3) and Java (verified openjdk 1.8.0_322).

You need to set the password for `org.apache.wss4j.crypto.merlin.keystore.password` in client.properties before you build.
Also if you have tested make sure that pom.xml is back to how it was.

To build a .jar file that can be used in Keycloak run the following command

```bash
mvn clean package
```

## Deploy

To deploy the user storage provider in Keycloak copy the .jar file into the `/opt/keycloak/providers` folder.

When deploying to Docker, copy the file before running `kc.sh build` in the Docker file.
