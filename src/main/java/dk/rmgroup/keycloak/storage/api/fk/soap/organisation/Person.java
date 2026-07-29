package dk.rmgroup.keycloak.storage.api.fk.soap.organisation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kombit.xml.schemas.requestheader._1.RequestHeaderType;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.SoapUtils;
import dk.stoettesystemerne.organisation._6.PersonPortType;
import dk.stoettesystemerne.organisation._6.PersonService;
import dk.stoettesystemerne.organisation.person._6.ListOutputType;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Holder;
import oio.sagdok._3_0.ListInputType;

public class Person {
  private static final Logger LOGGER = LoggerFactory.getLogger(Person.class);

  private final PersonPortType personPort;

  public Person() {
    personPort = new PersonService().getPerson();
    BindingProvider bindingProvider = (BindingProvider) personPort;
    bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        ClientProperties.getInstance().getPersonEndpointUrl());
  }

  private static Person person;

  /**
   * Methods that allow other classes to get an instance of Person
   * to avoid creating multiple instances of the same class
   * If an instance of Person does not exist, one is created
   *
   * @return Instance of Person
   */
  public static Person getPerson() {
    if (person == null)
      person = new Person();
    return person;
  }

  /**
   * Lists the persons for the given UUIDs.
   *
   * @param personUuids List of person UUIDs
   * @return ListOutputType containing the persons
   */
  public ListOutputType list(List<String> personUuids) {

    Holder<RequestHeaderType> requestHeader = SoapUtils.getRequestHeader();
    ListInputType listInputType = new ListInputType();
    listInputType.getUUIDIdentifikator().addAll(personUuids);

    ListOutputType listResponse;
    listResponse = personPort.list(requestHeader, listInputType);

    return listResponse;
  }
}
