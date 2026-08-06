package dk.rmgroup.keycloak.storage.api.fk.soap;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Adresse;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Bruger;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Organisation;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.OrganisationEnhed;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.OrganisationFunktion;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.OrganisationSystem;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Person;
import dk.rmgroup.keycloak.storage.api.fk.soap.organisation.Virksomhed;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;
import dk.stoettesystemerne.organisation.bruger._6.FiltreretOejebliksbilledeType;

public class Test {
  private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);

  private void run() throws IOException {
    ClientProperties clientProperties = ClientProperties.getInstance();

    // getResource only works with / in front of the String
    String keyStoreFile = Example.class.getResource('/' + clientProperties.getKeystoreFilename()).getFile();
    String trustStoreFile = Example.class.getResource('/' + clientProperties.getTruststoreFilename()).getFile();

    // Uncomment the line below to get more debugging info
    // System.setProperty("javax.net.debug", "ssl");

    // CXF uses Java built-in methods to sign requests to the token service, so we
    // have to give Java our certificate
    System.setProperty("javax.net.ssl.keyStore", keyStoreFile);
    System.setProperty("javax.net.ssl.keyStorePassword", clientProperties.getKeystorePassword());

    System.setProperty("javax.net.ssl.trustStore", trustStoreFile);
    System.setProperty("javax.net.ssl.trustStorePassword", clientProperties.getTruststorePassword());

    Organisation organisation = Organisation.getOrganisation();
    Virksomhed virksomhed = Virksomhed.getVirksomhed();
    Bruger bruger = Bruger.getBruger();
    Adresse adresse = Adresse.getAdresse();
    Person person = Person.getPerson();
    OrganisationSystem organisationSystem = OrganisationSystem.getOrganisationSystem();
    OrganisationEnhed organisationEnhed = OrganisationEnhed.getOrganisationEnhed();
    OrganisationFunktion organisationFunktion = OrganisationFunktion.getOrganisationFunktion();
    boolean run = true;
    while (run) {
      System.out.println("Press enter to refresh, or type 'exit' to quit.");
      String input = System.console().readLine();
      if ("exit".equalsIgnoreCase(input)) {
        run = false;
      } else if ("test".equalsIgnoreCase(input)) {
        var organisationResponse = organisation.getOrganisationByCvr("29188475");
        var userResponse = bruger.soeg(organisationResponse[1]);
        var listResponse = bruger.list(userResponse);
        var personUuids = listResponse.getFiltreretOejebliksbillede().stream()
            .<String>flatMap(x -> x.getRegistrering().stream().<String>flatMap(r -> r.getRelationListe()
                .getTilknyttedePersoner().stream().<String>map(p -> p.getReferenceID().getUUIDIdentifikator())))
            .toList();
        var personResponse = person.list(personUuids);

        var addressUuids = listResponse.getFiltreretOejebliksbillede().stream()
            .<String>flatMap(x -> x.getRegistrering().stream().<String>flatMap(r -> r.getRelationListe()
                .getAdresser().stream()
                .filter(a -> "Bruger.Adresse.Telefon".equals(a.getRolle().getLabel())
                    || "Bruger.Adresse.email".equals(a.getRolle().getLabel()))
                .<String>map(a -> a.getReferenceID().getUUIDIdentifikator())))
            .toList();

        var addressResponse = adresse.list(addressUuids);

        for (FiltreretOejebliksbilledeType user : listResponse.getFiltreretOejebliksbillede()) {
          var baseRegistration = user.getRegistrering().getFirst();
          var userName = baseRegistration.getAttributListe().getEgenskab().getFirst().getBrugerNavn();
          if("hjotest".equals(userName)) {
            var test = "test";
          }
          var attributeMobileId = baseRegistration.getRelationListe().getAdresser().stream()
              .filter(x -> "Bruger.Adresse.Telefon".equals(x.getRolle().getLabel())).findFirst()
              .<String>map(x -> x.getReferenceID().getUUIDIdentifikator()).orElse(null);
          var attributeEmailId = baseRegistration.getRelationListe().getAdresser().stream()
              .filter(x -> "Bruger.Adresse.email".equals(x.getRolle().getLabel())).findFirst()
              .<String>map(x -> x.getReferenceID().getUUIDIdentifikator()).orElse(null);
          try {
            String mobile = "";
            String email = "";

            if (attributeMobileId != null && !attributeMobileId.isEmpty()) {
              mobile = addressResponse.getFiltreretOejebliksbillede().stream()
                  .filter(x -> attributeMobileId.equals(x.getObjektType().getUUIDIdentifikator())).findFirst()
                  .<String>map(
                      x -> x.getRegistrering().getFirst().getAttributListe().getEgenskab().getFirst().getAdresseTekst())
                  .orElse(null);
            }

            if (attributeEmailId != null && !attributeEmailId.isEmpty()) {
              email = addressResponse.getFiltreretOejebliksbillede().stream()
                  .filter(x -> attributeEmailId.equals(x.getObjektType().getUUIDIdentifikator())).findFirst()
                  .<String>map(
                      x -> x.getRegistrering().getFirst().getAttributListe().getEgenskab().getFirst().getAdresseTekst())
                  .orElse(null);
            }

            var personName = personResponse.getFiltreretOejebliksbillede().stream()
                .filter(x -> baseRegistration.getRelationListe().getTilknyttedePersoner().stream().anyMatch(
                    p -> p.getReferenceID().getUUIDIdentifikator().equals(x.getObjektType().getUUIDIdentifikator())))
                .findFirst()
                .<String>map(
                    x -> x.getRegistrering().getFirst().getAttributListe().getEgenskab().getFirst().getNavnTekst())
                .orElse(null);

            System.out.println(
                "User: " + userName + ", Mobile: " + mobile + ", Email: " + email + ", Person Name: " + personName);
          } catch (Exception ex) {
            System.out.println("Error processing user: " + ex.getMessage());
          }
        }
      } else if ("test2".equalsIgnoreCase(input)) {
        var organisationResponse = organisation.getOrganisationByCvr("29189676");
        // var userResponse = bruger.soeg(organisationResponse[1]);
        // var listResponse =
        // bruger.list(userResponse.getIdListe().getUUIDIdentifikator());
        // var test = organisationSystem.fremsoegObjekthierarki();
        // var organisationsEnheder =
        // test.getOrganisationEnheder().getFiltreretOejebliksbillede().stream()
        // .<String>flatMap(x -> x.getRegistrering().stream()
        // .<String>flatMap(r ->
        // r.getAttributListe().getEgenskab().stream().<String>map(p ->
        // p.getEnhedNavn())))
        // .toList();

        var userResponse = bruger.soeg(organisationResponse[1]);

        var funktionSoegResponse = organisationFunktion.soeg(organisationResponse[1]);
        var funktionResponse = organisationFunktion.list(funktionSoegResponse);
        var funktioner = funktionResponse.getFiltreretOejebliksbillede().stream()
            .<String>flatMap(x -> x.getRegistrering().stream()
                .<String>flatMap(
                    r -> r.getAttributListe().getEgenskab().stream().<String>map(p -> p.getFunktionNavn())))
            .toList();
        var enhedUuids = funktionResponse.getFiltreretOejebliksbillede().stream()
            .<String>flatMap(x -> x.getRegistrering().stream()
                .<String>flatMap(r -> r.getRelationListe().getTilknyttedeEnheder().stream()
                    .<String>map(p -> p.getReferenceID().getUUIDIdentifikator())))
            .toList();
        var enhedResponse = organisationEnhed.list(enhedUuids);
        var enheder = enhedResponse.getFiltreretOejebliksbillede().stream()
            .<String>flatMap(x -> x.getRegistrering().stream()
                .<String>flatMap(r -> r.getAttributListe().getEgenskab().stream().<String>map(p -> p.getEnhedNavn())))
            .toList();
        System.out.println("Funktioner: " + funktioner);
        System.out.println("Enheder: " + enheder);
        var tes123 = "";

        // organisationEnhed.list(listResponse.getFiltreretOejebliksbillede().stream()
        // .<String>flatMap(x -> x.getRegistrering().stream().<String>flatMap(r ->
        // r.getRelationListe()
        // .getTilknyttedeEnheder().stream().<String>map(p ->
        // p.getReferenceID().getUUIDIdentifikator())))
        // .toList());
      } else if ("test3".equalsIgnoreCase(input)) {
        var organisationResponse = organisation.getOrganisationByCvr("29189676");

        // var test = organisationSystem.fremsoegObjekthierarki();
        // var organisationsEnheder =
        // test.getOrganisationEnheder().getFiltreretOejebliksbillede().stream()
        // .<String>flatMap(x -> x.getRegistrering().stream()
        // .<String>flatMap(r ->
        // r.getAttributListe().getEgenskab().stream().<String>map(p ->
        // p.getEnhedNavn())))
        // .toList();

        // var test2323 = test.getOrganisationFunktioner();

        // var funktioner =
        // test.getOrganisationFunktioner().getFiltreretOejebliksbillede().stream()
        // .<String>flatMap(x -> x.getRegistrering().stream()
        // .<String>flatMap(
        // r -> r.getAttributListe().getEgenskab().stream().<String>map(p ->
        // p.getFunktionNavn())))
        // .toList();

        // var funktioner2 =
        // test.getOrganisationEnheder().getFiltreretOejebliksbillede().stream()
        // .<String>flatMap(x -> x.getRegistrering().stream()
        // .<String>flatMap(r ->
        // r.getRelationListe().getTilknyttedeFunktioner().stream()
        // .<String>map(p -> p.getReferenceID().getUUIDIdentifikator())))
        // .toList();

        var enhedResponse = organisationEnhed.laes("5d029ecb-93a9-4bb4-9e48-c38cc7b9ce7f");
        var enhedResponse2 = organisationEnhed.laes("2098919f-406f-43b1-b64a-e397aa093ad9");
        enhedResponse.getFiltreretOejebliksbillede().getRegistrering().stream().forEach(r -> {
          r.getAttributListe().getEgenskab().stream().forEach(e -> {
            System.out.println("Enhed Navn: " + e.getEnhedNavn());
          });
        });

        var functionList = organisationFunktion.soeg2(organisationResponse[1], "5d029ecb-93a9-4bb4-9e48-c38cc7b9ce7f");
        var listResponse = organisationFunktion.list(functionList);

        var test2 = "test";

      } else if ("test4".equalsIgnoreCase(input)) {
        var fetcher = new UserFetcherService("29188475", "Bruger.Adresse.email", "Bruger.Adresse.Telefon");
        var fetcher2 = new UserFetcherService("18957981", "Bruger.Adresse.email", "Bruger.Adresse.Telefon");
        var unitNames = new ArrayList<String>();
        //unitNames.add("Gåsetårnskolen Iselinge Indskoling");
        unitNames.add("5d029ecb-93a9-4bb4-9e48-c38cc7b9ce7f");

        //var unitMap = fetcher.getUnitMap(unitNames);

        //var users = fetcher.fetchUsersInUnit(unitNames);

        var users = fetcher.fetchAllUsers();
        var users2 = fetcher2.fetchAllUsers();

        // users.forEach(u -> {
        //   System.out.println("User: " + u.getUserName() + ", Mobile: " + u.getMobile() + ", Email: " + u.getEmail()
        //       + ", Person Name: " + u.getPersonName());
        // });

        System.out.println("Users from CVR 29188475: " + users.size());
        System.out.println("Users from CVR 18957981: " + users2.size());

        var test = "ts";

      } else {
        var test = organisation.getOrganisationByCvr("11111111");
        var tes2 = virksomhed.getVirksomhedUuid("11111111");
      }
    }
  }

  public static void main(String[] args) throws IOException {
    new Test().run();
  }
}
