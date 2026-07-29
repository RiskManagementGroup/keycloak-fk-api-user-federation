package dk.rmgroup.keycloak.storage.api.fk.soap.organisation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kombit.xml.schemas.requestheader._1.RequestHeaderType;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.SoapUtils;
import dk.stoettesystemerne.organisation._6.GyldighedStatusKodeType;
import dk.stoettesystemerne.organisation._6.GyldighedType;
import dk.stoettesystemerne.organisation._6.OrganisationEnhedPortType;
import dk.stoettesystemerne.organisation._6.OrganisationEnhedService;
import dk.stoettesystemerne.organisation.organisationenhed._6.AttributListeType;
import dk.stoettesystemerne.organisation.organisationenhed._6.EgenskabType;
import dk.stoettesystemerne.organisation.organisationenhed._6.LaesOutputType;
import dk.stoettesystemerne.organisation.organisationenhed._6.ListOutputType;
import dk.stoettesystemerne.organisation.organisationenhed._6.RelationListeType;
import dk.stoettesystemerne.organisation.organisationenhed._6.SoegInputType;
import dk.stoettesystemerne.organisation.organisationenhed._6.TilstandListeType;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Holder;
import oio.sagdok._3_0.LaesInputType;
import oio.sagdok._3_0.ListInputType;
import oio.sagdok._3_0.OrganisationFlerRelationType;
import oio.sagdok._3_0.SoegOutputType;
import oio.sagdok._3_0.UnikIdType;

public class OrganisationEnhed {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationEnhed.class);

  private final OrganisationEnhedPortType organisationEnhedPort;

  public OrganisationEnhed() {
    organisationEnhedPort = new OrganisationEnhedService().getOrganisationEnhed();
    BindingProvider bindingProvider = (BindingProvider) organisationEnhedPort;
    bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        ClientProperties.getInstance().getOrganisationEnhedEndpointUrl());
  }

  private static OrganisationEnhed organisationEnhed;

  /**
   * Methods that allow other classes to get an instance of OrganisationEnhed
   * to avoid creating multiple instances of the same class
   * If an instance of OrganisationEnhed does not exist, one is created
   *
   * @return Instance of OrganisationEnhed
   */
  public static OrganisationEnhed getOrganisationEnhed() {
    if (organisationEnhed == null)
      organisationEnhed = new OrganisationEnhed();
    return organisationEnhed;
  }

  /**
   * Lists the users for the given UUIDs.
   *
   * @param userUuids Array of user UUIDs
   * @return ListOutputType containing the users
   */
  public ListOutputType list(List<String> userUuids) {

    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    ListInputType listInputType = new ListInputType();
    listInputType.getUUIDIdentifikator().addAll(userUuids);

    ListOutputType listResponse;
    listResponse = organisationEnhedPort.list(requestHeader, listInputType);

    return listResponse;
  }

  /**
   * Reads the details of a enhed for the given UUID.
   *
   * @param uuid UUID of the enhed
   * @return LaesOutputType containing the enhed
   */
  public LaesOutputType laes(String uuid) {

    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    LaesInputType laesInputType = new LaesInputType();
    laesInputType.setUUIDIdentifikator(uuid);

    LaesOutputType laesResponse;
    laesResponse = organisationEnhedPort.laes(requestHeader, laesInputType);

    return laesResponse;
  }

  public List<String> soeg(String organisationUuid, String enhedNavn) {
    var ids = new ArrayList<String>();
    int totalLength = 0;
    do {
      var response = soeg(organisationUuid, enhedNavn, BigInteger.valueOf(totalLength));
      if (response.getStandardRetur().getStatusKode().equals(BigInteger.valueOf(44))) {
        break; // No more results
      } else if (!response.getStandardRetur().getStatusKode().equals(BigInteger.valueOf(20))) {
        throw new RuntimeException("Error in soeg response: " + response.getStandardRetur().getStatusKode() + " - " + response.getStandardRetur().getFejlbeskedTekst());
      }
      totalLength += response.getIdListe().getUUIDIdentifikator().size();
      ids.addAll(response.getIdListe().getUUIDIdentifikator());
    } while (totalLength % 1000 == 0 && totalLength != 0);
    return ids;
  }

  public SoegOutputType soeg(String organisationUuid, String enhedNavn, BigInteger foersteResultatReference) {
    if (foersteResultatReference == null) {
      foersteResultatReference = BigInteger.ZERO;
    }
    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    SoegInputType soegInputType = new SoegInputType();
    var attributListe = new AttributListeType();
    var egenskabType = new EgenskabType();
    egenskabType.setEnhedNavn(enhedNavn);
    attributListe.getEgenskab().add(egenskabType);
    var gyldighed = new GyldighedType();
    gyldighed.setGyldighedStatusKode(GyldighedStatusKodeType.AKTIV);
    var tilstandListe = new TilstandListeType();
    tilstandListe.getGyldighed().add(gyldighed);
    var relationListe = new RelationListeType();
    var tilhoerer = new OrganisationFlerRelationType();
    var referenceID = new UnikIdType();
    referenceID.setUUIDIdentifikator(organisationUuid);
    tilhoerer.setReferenceID(referenceID);
    relationListe.setTilhoerer(tilhoerer);
    soegInputType.setAttributListe(attributListe);
    soegInputType.setTilstandListe(tilstandListe);
    soegInputType.setRelationListe(relationListe);
    soegInputType.setFoersteResultatReference(foersteResultatReference);

    SoegOutputType soegResponse;
    soegResponse = organisationEnhedPort.soeg(requestHeader, soegInputType);

    return soegResponse;
  }
}
