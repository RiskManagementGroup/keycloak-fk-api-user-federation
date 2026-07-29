package dk.rmgroup.keycloak.storage.api.fk.soap.interceptor.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class SbfFrameworkHeader {

    @XmlAttribute(name = "version")
    private final String version = "2.0";

    @XmlAttribute(name = "profile", namespace = "urn:liberty:sb:profile")
    private final String profile = "urn:liberty:sb:profile:basic";

    public String getVersion() {
        return version;
    }

    public String getProfile() {
        return profile;
    }
}
