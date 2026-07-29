package dk.rmgroup.keycloak.storage.api.fk.soap.organisation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kombit.xml.schemas.requestheader._1.RequestHeaderType;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.SoapUtils;
import dk.stoettesystemerne.organisation._6.BrugerPortType;
import dk.stoettesystemerne.organisation._6.BrugerService;
import dk.stoettesystemerne.organisation._6.GyldighedStatusKodeType;
import dk.stoettesystemerne.organisation._6.GyldighedType;
import dk.stoettesystemerne.organisation.bruger._6.AttributListeType;
import dk.stoettesystemerne.organisation.bruger._6.ListOutputType;
import dk.stoettesystemerne.organisation.bruger._6.RelationListeType;
import dk.stoettesystemerne.organisation.bruger._6.SoegInputType;
import dk.stoettesystemerne.organisation.bruger._6.TilstandListeType;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Holder;
import oio.sagdok._3_0.ListInputType;
import oio.sagdok._3_0.OrganisationFlerRelationType;
import oio.sagdok._3_0.SoegOutputType;
import oio.sagdok._3_0.UnikIdType;

public class Bruger {
  private static final Logger LOGGER = LoggerFactory.getLogger(Bruger.class);

  private final BrugerPortType brugerPort;

  public Bruger() {
    brugerPort = new BrugerService().getBruger();
    BindingProvider bindingProvider = (BindingProvider) brugerPort;
    bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        ClientProperties.getInstance().getBrugerEndpointUrl());
  }

  private static Bruger bruger;

  /**
   * Methods that allow other classes to get an instance of Bruger
   * to avoid creating multiple instances of the same class
   * If an instance of Bruger does not exist, one is created
   *
   * @return Instance of Bruger
   */
  public static Bruger getBruger() {
    if (bruger == null)
      bruger = new Bruger();
    return bruger;
  }

  /**
   * Searches for a Bruger object related to a given Organisation object.
   * Uses the SEARCH operation in OrganisationService
   *
   * @param organisationUuid UUID for the Organisation object to search for
   * @return Search output including UUID for the Organisation object
   */
  public List<String> soeg(String organisationUuid) {
    var ids = new ArrayList<String>();
    int totalLength = 0;
    do {
      var response = soeg(organisationUuid, BigInteger.valueOf(totalLength));
      totalLength += response.getIdListe().getUUIDIdentifikator().size();
      ids.addAll(response.getIdListe().getUUIDIdentifikator());
    } while (totalLength % 1000 == 0 && totalLength != 0);
    return ids;
  }

  /**
   * Searches for a Bruger object related to a given Organisation object.
   * Uses the SEARCH operation in OrganisationService
   *
   * @param organisationUuid         UUID for the Organisation object to search
   *                                 for
   * @param foersteResultatReference Reference for the first result in the search
   * @return Search output including UUID for the Organisation object
   */
  public SoegOutputType soeg(String organisationUuid, BigInteger foersteResultatReference) {
    if (foersteResultatReference == null) {
      foersteResultatReference = BigInteger.ZERO;
    }
    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    SoegInputType soegInputType = new SoegInputType();
    var attributListe = new AttributListeType();
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
    soegResponse = brugerPort.soeg(requestHeader, soegInputType);

    return soegResponse;
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
    listResponse = brugerPort.list(requestHeader, listInputType);

    return listResponse;
  }
}
