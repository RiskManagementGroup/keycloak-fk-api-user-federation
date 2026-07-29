package dk.rmgroup.keycloak.storage.api.fk.soap;

import java.util.HashSet;
import java.util.Set;

public class SimpleUser {
  private final String userName;
  private final String mobile;
  private final String email;
  private final String personName;
  private final Set<String> groups;

  public SimpleUser(String userName, String mobile, String email, String personName) {
    this.userName = userName;
    this.mobile = mobile;
    this.email = email;
    this.personName = personName;
    this.groups = new HashSet<>();
  }

  public String getUserName() {
    return userName;
  }

  public String getMobile() {
    return mobile;
  }

  public String getEmail() {
    return email;
  }

  public String getPersonName() {
    return personName;
  }

  public Set<String> getGroups() {
    return groups;
  }

  public void addGroup(String group) {
    if (!groups.contains(group)) {
      groups.add(group);
    }
  }
}
