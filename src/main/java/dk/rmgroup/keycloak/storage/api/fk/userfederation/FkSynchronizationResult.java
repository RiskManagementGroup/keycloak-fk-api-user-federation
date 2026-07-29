package dk.rmgroup.keycloak.storage.api.fk.userfederation;

import org.keycloak.storage.user.SynchronizationResult;

public class FkSynchronizationResult extends SynchronizationResult {
  private int fetched;

  public int getFetched() {
    return fetched;
  }

  public void setFetched(int fetched) {
    this.fetched = fetched;
  }

  @Override
  public String getStatus() {
    String status = super.getStatus();

    if (fetched > 0) {
      status = String.format("%d fetched users, ", fetched) + status;
    }

    return status;
  }
}
