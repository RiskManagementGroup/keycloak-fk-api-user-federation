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
import dk.stoettesystemerne.organisation._6.OrganisationFunktionPortType;
import dk.stoettesystemerne.organisation._6.OrganisationFunktionService;
import dk.stoettesystemerne.organisation.organisationfunktion._6.AttributListeType;
import dk.stoettesystemerne.organisation.organisationfunktion._6.ListOutputType;
import dk.stoettesystemerne.organisation.organisationfunktion._6.RelationListeType;
import dk.stoettesystemerne.organisation.organisationfunktion._6.SoegInputType;
import dk.stoettesystemerne.organisation.organisationfunktion._6.TilstandListeType;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Holder;
import oio.sagdok._3_0.ListInputType;
import oio.sagdok._3_0.OrganisationEnhedFlerRelationType;
import oio.sagdok._3_0.OrganisationFlerRelationType;
import oio.sagdok._3_0.SoegOutputType;
import oio.sagdok._3_0.UnikIdType;

public class OrganisationFunktion {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationFunktion.class);

  private final OrganisationFunktionPortType organisationFunktionPort;

  public OrganisationFunktion() {
    organisationFunktionPort = new OrganisationFunktionService().getOrganisationFunktion();
    BindingProvider bindingProvider = (BindingProvider) organisationFunktionPort;
    bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        ClientProperties.getInstance().getOrganisationFunktionEndpointUrl());
  }

  private static OrganisationFunktion organisationFunktion;

  /**
   * Methods that allow other classes to get an instance of OrganisationFunktion
   * to avoid creating multiple instances of the same class
   * If an instance of OrganisationFunktion does not exist, one is created
   *
   * @return Instance of OrganisationFunktion
   */
  public static OrganisationFunktion getOrganisationFunktion() {
    if (organisationFunktion == null)
      organisationFunktion = new OrganisationFunktion();
    return organisationFunktion;
  }

  public List<String> soeg(String organisationUuid) {
    var ids = new ArrayList<String>();
    int totalLength = 0;
    do {
      var response = soeg(organisationUuid, BigInteger.valueOf(totalLength));
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

  /**
   * Searches for a Funktion object related to a given Organisation object.
   * Uses the SEARCH operation in OrganisationService
   *
   * @param organisationUuid         UUID for the Organisation object to search
   *                                 for
   * @param userUuids                List of user UUIDs to filter the search
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
    relationListe.getTilknyttedeOrganisationer().add(tilhoerer);
    soegInputType.setAttributListe(attributListe);
    soegInputType.setTilstandListe(tilstandListe);
    soegInputType.setRelationListe(relationListe);
    soegInputType.setFoersteResultatReference(foersteResultatReference);

    SoegOutputType soegResponse;
    soegResponse = organisationFunktionPort.soeg(requestHeader, soegInputType);

    return soegResponse;
  }

  public List<String> soeg2(String organisationUuid, String enhedUuid) {
    var ids = new ArrayList<String>();
    int totalLength = 0;
    do {
      var response = soeg2(organisationUuid, enhedUuid, BigInteger.valueOf(totalLength));
      if (response.getStandardRetur().getStatusKode().equals(BigInteger.valueOf(44))) {
        break; // No more results
      } else if (!response.getStandardRetur().getStatusKode().equals(BigInteger.valueOf(20))) {
        throw new RuntimeException("Error in soeg response: " + response.getStandardRetur().getStatusKode() + " - "
            + response.getStandardRetur().getFejlbeskedTekst());
      }
      totalLength += response.getIdListe().getUUIDIdentifikator().size();
      ids.addAll(response.getIdListe().getUUIDIdentifikator());
    } while (totalLength % 1000 == 0 && totalLength != 0);
    return ids;
  }

  public SoegOutputType soeg2(String organisationUuid, String enhedUuid, BigInteger foersteResultatReference) {
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
    relationListe.getTilknyttedeOrganisationer().add(tilhoerer);
    var tilhoererEnhed = new OrganisationEnhedFlerRelationType();
    var referenceIDEnhed = new UnikIdType();
    referenceIDEnhed.setUUIDIdentifikator(enhedUuid);
    tilhoererEnhed.setReferenceID(referenceIDEnhed);
    relationListe.getTilknyttedeEnheder().add(tilhoererEnhed);
    soegInputType.setAttributListe(attributListe);
    soegInputType.setTilstandListe(tilstandListe);
    soegInputType.setRelationListe(relationListe);
    soegInputType.setFoersteResultatReference(foersteResultatReference);

    SoegOutputType soegResponse;
    soegResponse = organisationFunktionPort.soeg(requestHeader, soegInputType);

    return soegResponse;
  }

  /**
   * Lists the funktioner for the given UUIDs.
   *
   * @param userUuids Array of user UUIDs
   * @return ListOutputType containing the funktioner
   */
  public ListOutputType list(List<String> userUuids) {

    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    ListInputType listInputType = new ListInputType();
    listInputType.getUUIDIdentifikator().addAll(userUuids);

    ListOutputType listResponse;
    listResponse = organisationFunktionPort.list(requestHeader, listInputType);

    return listResponse;
  }
}
