package dk.rmgroup.keycloak.storage.api.fk.soap.organisation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kombit.xml.schemas.requestheader._1.RequestHeaderType;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.SoapUtils;
import dk.stoettesystemerne.organisation._6.AdressePortType;
import dk.stoettesystemerne.organisation._6.AdresseService;
import dk.stoettesystemerne.organisation.adresse._6.ListOutputType;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Holder;
import oio.sagdok._3_0.ListInputType;

public class Adresse {

  private static final Logger LOGGER = LoggerFactory.getLogger(Adresse.class);

  private final AdressePortType adressePort;

  public Adresse() {
    adressePort = new AdresseService().getAdresse();
    BindingProvider bindingProvider = (BindingProvider) adressePort;
    bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        ClientProperties.getInstance().getAdresseEndpointUrl());
  }

  private static Adresse adresse;

  /**
   * Methods that allow other classes to get an instance of Adresse
   * to avoid creating multiple instances of the same class
   * If an instance of Adresse does not exist, one is created
   *
   * @return Instance of Adresse
   */
  public static Adresse getAdresse() {
    if (adresse == null)
      adresse = new Adresse();
    return adresse;
  }

  /**
   * Lists the addresses for the given UUIDs.
   *
   * @param adresseUuids List of address UUIDs
   * @return ListOutputType containing the addresses
   */
  public ListOutputType list(List<String> adresseUuids) {

    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    ListInputType listInputType = new ListInputType();
    listInputType.getUUIDIdentifikator().addAll(adresseUuids);

    ListOutputType listResponse;
    listResponse = adressePort.list(requestHeader, listInputType);

    return listResponse;
  }
}
