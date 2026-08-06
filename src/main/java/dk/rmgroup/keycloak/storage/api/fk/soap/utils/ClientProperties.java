package dk.rmgroup.keycloak.storage.api.fk.soap.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientProperties {

    private static final String PROPERTIES_NAME = "/client.properties";
    private static ClientProperties clientProperties;
    private final Properties properties;

    // Variables which MUST BE MODIFIED before running the code examples

    private static final String KEYSTORE_FILENAME = "org.apache.wss4j.crypto.merlin.keystore.file";
    private static final String KEYSTORE_PASSWORD = "org.apache.wss4j.crypto.merlin.keystore.password";
    private static final String TRUSTSTORE_FILENAME = "org.apache.wss4j.crypto.merlin.truststore.file";
    private static final String TRUSTSTORE_PASSWORD = "org.apache.wss4j.crypto.merlin.truststore.password";

    // Variables for endpoints - CAN be modified

    private static final String SAGDOKUMENT_INDEKS_ENDPOINT_URL = "sagdokumentIndeksEndpointUrl";
    private static final String KLASSIFIKATION_ENDPOINT_URL = "klassifikationEndpointUrl";
    private static final String ORGANISATION_ENDPOINT_URL = "organisationEndpointUrl";
    private static final String VIRKSOMHED_ENDPOINT_URL = "virksomhedEndpointUrl";
    private static final String BRUGER_ENDPOINT_URL = "brugerEndpointUrl";
    private static final String ADRESSE_ENDPOINT_URL = "adresseEndpointUrl";
    private static final String PERSON_ENDPOINT_URL = "personEndpointUrl";
    private static final String ORGANISATION_ENHED_ENDPOINT_URL = "organisationEnhedEndpointUrl";
    private static final String ORGANISATION_SYSTEM_ENDPOINT_URL = "organisationSystemEndpointUrl";
    private static final String ORGANISATION_FUNKTION_ENDPOINT_URL = "organisationFunktionEndpointUrl";

    /**
     * Method creates a new instance of ClientProperties
     * 
     * @return clientProperties
     */
    public static ClientProperties getInstance() {
        if (clientProperties == null) {
            clientProperties = new ClientProperties();
        }

        return clientProperties;
    }

    /**
     * Method is responsible for reading the properties from the client.properties
     * file
     */
    private ClientProperties() {
        properties = new Properties();

        try (InputStream inputStream = getClass().getResourceAsStream(PROPERTIES_NAME)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + PROPERTIES_NAME);
        }
    }

    /**
     * Get methods for variables and constansts
     * 
     * @return varibles and constants
     */

    public String getKeystoreFilename() {
        return properties.getProperty(KEYSTORE_FILENAME);
    }

    public String getKeystorePassword() {
        return properties.getProperty(KEYSTORE_PASSWORD);
    }

    public String getTruststoreFilename() {
        return properties.getProperty(TRUSTSTORE_FILENAME);
    }

    public String getTruststorePassword() {
        return properties.getProperty(TRUSTSTORE_PASSWORD);
    }

    public String getSagdokumentIndeksEndpointUrl() {
        return properties.getProperty(SAGDOKUMENT_INDEKS_ENDPOINT_URL);
    }

    public String getKlassifikationEndpointUrl() {
        return properties.getProperty(KLASSIFIKATION_ENDPOINT_URL);
    }

    public String getOrganisationEndpointUrl() {
        return properties.getProperty(ORGANISATION_ENDPOINT_URL);
    }

    public String getVirksomhedEndpointUrl() {
        return properties.getProperty(VIRKSOMHED_ENDPOINT_URL);
    }

    public String getBrugerEndpointUrl() {
        return properties.getProperty(BRUGER_ENDPOINT_URL);
    }

    public String getAdresseEndpointUrl() {
        return properties.getProperty(ADRESSE_ENDPOINT_URL);
    }

    public String getPersonEndpointUrl() {
        return properties.getProperty(PERSON_ENDPOINT_URL);
    }

    public String getOrganisationEnhedEndpointUrl() {
        return properties.getProperty(ORGANISATION_ENHED_ENDPOINT_URL);
    }

    public String getOrganisationSystemEndpointUrl() {
        return properties.getProperty(ORGANISATION_SYSTEM_ENDPOINT_URL);
    }

    public String getOrganisationFunktionEndpointUrl() {
        return properties.getProperty(ORGANISATION_FUNKTION_ENDPOINT_URL);
    }
}
