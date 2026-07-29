package dk.rmgroup.keycloak.storage.api.fk.userfederation;

import java.util.List;

import org.keycloak.storage.user.SynchronizationResult;

public class FkApiUserResult {
  public final SynchronizationResult synchronizationResult;
  public final List<String> errors;

  public FkApiUserResult(SynchronizationResult synchronizationResult, List<String> errors) {
    this.synchronizationResult = synchronizationResult;
    this.errors = errors;
  }
}
