package dk.rmgroup.keycloak.storage.api.fk.soap.organisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kombit.xml.schemas.requestheader._1.RequestHeaderType;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.SoapUtils;
import dk.stoettesystemerne.organisation._6.OrganisationSystemPortType;
import dk.stoettesystemerne.organisation._6.OrganisationSystemService;
import dk.stoettesystemerne.organisation.organisation._6.EgenskabType;
import dk.stoettesystemerne.organisation.organisationsystem._6.FremsoegObjekthierarkiInputType;
import dk.stoettesystemerne.organisation.organisationsystem._6.FremsoegObjekthierarkiOutputType;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Holder;

public class OrganisationSystem {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationSystem.class);

  private final OrganisationSystemPortType organisationSystemPort;

  public OrganisationSystem() {
    organisationSystemPort = new OrganisationSystemService().getOrganisationSystem();
    BindingProvider bindingProvider = (BindingProvider) organisationSystemPort;
    bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        ClientProperties.getInstance().getOrganisationSystemEndpointUrl());
  }

  private static OrganisationSystem organisationSystem;

  /**
   * Methods that allow other classes to get an instance of OrganisationSystem
   * to avoid creating multiple instances of the same class
   * If an instance of OrganisationSystem does not exist, one is created
   *
   * @return Instance of OrganisationSystem
   */
  public static OrganisationSystem getOrganisationSystem() {
    if (organisationSystem == null)
      organisationSystem = new OrganisationSystem();
    return organisationSystem;
  }

  public FremsoegObjekthierarkiOutputType fremsoegObjekthierarki(String organisationNavn) {
    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    FremsoegObjekthierarkiInputType fremsoegObjekthierarkiInputType = new FremsoegObjekthierarkiInputType();
    var organisationSoegEgenskab = new EgenskabType();
    organisationSoegEgenskab.setOrganisationNavn(organisationNavn);
    fremsoegObjekthierarkiInputType.setOrganisationSoegEgenskab(organisationSoegEgenskab);
    var organisationEnhedSoegEgenskab = new dk.stoettesystemerne.organisation.organisationenhed._6.EgenskabType();
    fremsoegObjekthierarkiInputType.setOrganisationEnhedSoegEgenskab(organisationEnhedSoegEgenskab);

    FremsoegObjekthierarkiOutputType response;
    response = organisationSystemPort.fremsoegobjekthierarki(requestHeader, fremsoegObjekthierarkiInputType);

    return response;
  }
}
