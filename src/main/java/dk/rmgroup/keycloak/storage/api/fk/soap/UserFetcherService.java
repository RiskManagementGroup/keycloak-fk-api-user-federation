package dk.rmgroup.keycloak.storage.api.fk.soap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Adresse;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Bruger;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Organisation;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.OrganisationEnhed;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.OrganisationFunktion;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Person;
import dk.stoettesystemerne.organisation._6.GyldighedStatusKodeType;
import dk.stoettesystemerne.organisation.bruger._6.FiltreretOejebliksbilledeType;

public class UserFetcherService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserFetcherService.class);

  private final String emailFieldName;
  private final String mobileFieldName;
  private final String cvrNumber;
  private final String organisationUuid;

  public UserFetcherService(String cvrNumber, String emailFieldName, String mobileFieldName) {
    this.cvrNumber = cvrNumber;
    this.emailFieldName = emailFieldName;
    this.mobileFieldName = mobileFieldName;
    this.organisationUuid = getOrganisationUuid(cvrNumber);
  }

  public final String getOrganisationUuid(String cvr) {
    Organisation organisation = Organisation.getOrganisation();
    return organisation.getOrganisationByCvr(cvr)[1];
  }

  public List<SimpleUser> fetchAllUsers() {
    Bruger bruger = Bruger.getBruger();

    var userResponse = bruger.soeg(organisationUuid);
    var listResponse = bruger.list(userResponse);

    return addPropsToUser(listResponse.getFiltreretOejebliksbillede());
  }

  public Map<String, String> getUnitMap(List<String> unitNames) {
    Map<String, String> unitMap = new java.util.HashMap<>();
    OrganisationEnhed organisationEnhed = OrganisationEnhed.getOrganisationEnhed();

    Set<String> groupMapUUIDSet = unitNames.stream().filter(k -> {
      try {
        UUID.fromString(k);
        return true;
      } catch (Exception e) {
        return false;
      }
    }).collect(Collectors.toSet());

    Set<String> groupMapDisplayNameSet = new HashSet<>(unitNames);
    groupMapDisplayNameSet.removeAll(groupMapUUIDSet);

    if (!groupMapUUIDSet.isEmpty()) {
      for (String unitUuid : groupMapUUIDSet) {
        var laesResponse = organisationEnhed.laes(unitUuid);
        String unitName = laesResponse.getFiltreretOejebliksbillede().getRegistrering().getFirst().getAttributListe()
            .getEgenskab().getFirst().getEnhedNavn();
        unitMap.put(unitUuid, unitName);
      }
    }

    if (!groupMapDisplayNameSet.isEmpty()) {
      for (String unitName : groupMapDisplayNameSet) {
        var soegResponse = organisationEnhed.soeg(organisationUuid, unitName);
        for (var unitUuid : soegResponse) {
          unitMap.put(unitUuid, unitName);
        }
      }
    }

    return unitMap;
  }

  public List<SimpleUser> fetchUsersInUnit(List<String> unitNames) {
    OrganisationEnhed organisationEnhed = OrganisationEnhed.getOrganisationEnhed();

    var unitUuids = new ArrayList<String>();

    for (String unitName : unitNames) {
      var soegResponse = organisationEnhed.soeg(organisationUuid, unitName);
      unitUuids.addAll(soegResponse);
    }

    return fetchUsersInUnitUuids(unitUuids);
  }

  public List<SimpleUser> fetchUsersInUnitUuids(List<String> unitUuids) {
    Bruger bruger = Bruger.getBruger();
    OrganisationFunktion organisationFunktion = OrganisationFunktion.getOrganisationFunktion();

    var functionUuids = new ArrayList<String>();

    for (String unitUuid : unitUuids) {
      var funktionSoegResponse = organisationFunktion.soeg2(organisationUuid, unitUuid);
      functionUuids.addAll(funktionSoegResponse);
    }

    var funktionResponse = organisationFunktion.list(functionUuids);

    List<String> userUuids;
    userUuids = funktionResponse.getFiltreretOejebliksbillede().stream()
        .<String>flatMap(x -> x.getRegistrering().stream()
            .filter(r -> r.getTilstandListe().getGyldighed().stream()
                .anyMatch(g -> g.getGyldighedStatusKode() == GyldighedStatusKodeType.AKTIV))
            .<String>flatMap(r -> r.getRelationListe()
                .getTilknyttedeBrugere().stream().<String>map(p -> p.getReferenceID().getUUIDIdentifikator())))
        .toList();

    var listResponse = bruger.list(userUuids);

    var filteredUsers = listResponse.getFiltreretOejebliksbillede().stream().filter(u -> u.getRegistrering().stream()
        .anyMatch(r -> r.getTilstandListe().getGyldighed().stream()
            .anyMatch(g -> g.getGyldighedStatusKode() == GyldighedStatusKodeType.AKTIV)))
        .toList();

    return addPropsToUser(filteredUsers);
  }

  public List<SimpleUser> addPropsToUser(List<FiltreretOejebliksbilledeType> users) {
    Adresse adresse = Adresse.getAdresse();
    Person person = Person.getPerson();

    var personUuids = users.stream()
        .<String>flatMap(x -> x.getRegistrering().stream().<String>flatMap(r -> r.getRelationListe()
            .getTilknyttedePersoner().stream().<String>map(p -> p.getReferenceID().getUUIDIdentifikator())))
        .toList();
    var personResponse = person.list(personUuids);

    var addressUuids = users.stream()
        .<String>flatMap(x -> x.getRegistrering().stream().<String>flatMap(r -> r.getRelationListe()
            .getAdresser().stream()
            .filter(a -> mobileFieldName.equals(a.getRolle().getLabel())
                || emailFieldName.equals(a.getRolle().getLabel()))
            .<String>map(a -> a.getReferenceID().getUUIDIdentifikator())))
        .toList();

    var addressResponse = adresse.list(addressUuids);

    List<SimpleUser> simpleUsers = new ArrayList<>();

    for (FiltreretOejebliksbilledeType user : users) {
      String userName = "";
      String email = "";

      try {
        var baseRegistration = user.getRegistrering().getFirst();
        userName = baseRegistration.getAttributListe().getEgenskab().getFirst().getBrugerNavn();
        var attributeMobileId = baseRegistration.getRelationListe().getAdresser().stream()
            .filter(x -> mobileFieldName.equals(x.getRolle().getLabel())).findFirst()
            .<String>map(x -> x.getReferenceID().getUUIDIdentifikator()).orElse(null);
        var attributeEmailId = baseRegistration.getRelationListe().getAdresser().stream()
            .filter(x -> emailFieldName.equals(x.getRolle().getLabel())).findFirst()
            .<String>map(x -> x.getReferenceID().getUUIDIdentifikator()).orElse(null);
        String mobile = "";

        if (attributeMobileId != null && !attributeMobileId.isEmpty()) {
          mobile = addressResponse.getFiltreretOejebliksbillede().stream()
              .filter(x -> attributeMobileId.equals(x.getObjektType().getUUIDIdentifikator())).findFirst()
              .<String>map(
                  x -> x.getRegistrering().getFirst().getAttributListe().getEgenskab().getFirst()
                      .getAdresseTekst())
              .orElse(null);
        }

        if (attributeEmailId != null && !attributeEmailId.isEmpty()) {
          email = addressResponse.getFiltreretOejebliksbillede().stream()
              .filter(x -> attributeEmailId.equals(x.getObjektType().getUUIDIdentifikator())).findFirst()
              .<String>map(
                  x -> x.getRegistrering().getFirst().getAttributListe().getEgenskab().getFirst()
                      .getAdresseTekst())
              .orElse(null);
        }

        var personName = personResponse.getFiltreretOejebliksbillede().stream()
            .filter(x -> baseRegistration.getRelationListe().getTilknyttedePersoner().stream().anyMatch(
                p -> p.getReferenceID().getUUIDIdentifikator()
                    .equals(x.getObjektType().getUUIDIdentifikator())))
            .findFirst()
            .<String>map(
                x -> x.getRegistrering().getFirst().getAttributListe().getEgenskab().getFirst()
                    .getNavnTekst())
            .orElse(null);

        if (email == null || email.isEmpty()) {
          continue;
        }

        simpleUsers.add(new SimpleUser(email, mobile, email, personName));
      } catch (Exception ex) {
        LOGGER.error("Error processing user: " + userName + " " + email, ex);
      }
    }

    return simpleUsers;
  }
}
