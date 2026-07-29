package dk.rmgroup.keycloak.storage.api.fk.userfederation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.StoreSyncEvent;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;

import com.google.common.base.Strings;

import dk.rmgroup.keycloak.storage.api.fk.soap.SimpleUser;
import dk.rmgroup.keycloak.storage.api.fk.soap.UserFetcherService;
import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_ALLOW_UPDATE_UPN_DOMAINS;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_CVR_NUMBER;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_DO_NOT_OVERRIDE_MOBILE_WITH_EMPTY;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_EMAIL_FIELD_NAME;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_GROUPS_FOR_USERS_NOT_IN_MAPPED_GROUPS;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_GROUP_MAP;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_IMPORT_USERS_NOT_IN_MAPPED_GROUPS;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_MOBILE_FIELD_NAME;
import static dk.rmgroup.keycloak.storage.api.fk.userfederation.FkApiUserStorageProvideConstants.CONFIG_KEY_ONLY_USE_GROUPS_IN_GROUP_MAP;

public class FkApiUserStorageProviderFactory
    implements UserStorageProviderFactory<FkApiUserStorageProvider>, ImportSynchronization {

  protected final List<ProviderConfigProperty> configMetadata;

  private static final Logger logger = Logger.getLogger(FkApiUserStorageProviderFactory.class);

  private static final int USER_REMOVE_PAGE_SIZE = 100;

  private static final int USER_IMPORT_PAGE_SIZE = 100;

  public FkApiUserStorageProviderFactory() {
    configMetadata = ProviderConfigurationBuilder.create()
        .property()
        .name(CONFIG_KEY_CVR_NUMBER)
        .label("CVR Number")
        .type(ProviderConfigProperty.STRING_TYPE)
        .helpText("CVR Number for the organization")
        .add()
        .property()
        .name(CONFIG_KEY_EMAIL_FIELD_NAME)
        .label("Email field name")
        .type(ProviderConfigProperty.STRING_TYPE)
        .helpText("Email field name for the organization. Is normally 'Bruger.Adresse.email'")
        .add()
        .property()
        .name(CONFIG_KEY_MOBILE_FIELD_NAME)
        .label("Mobile field name")
        .type(ProviderConfigProperty.STRING_TYPE)
        .helpText("Mobile field name for the organization. Is normally 'Bruger.Adresse.Telefon'")
        .add()
        .property()
        .name(CONFIG_KEY_ALLOW_UPDATE_UPN_DOMAINS)
        .label("Allow taking over users from UPN domains")
        .type(ProviderConfigProperty.STRING_TYPE)
        .helpText(
            "Allow taking over federation for users whose UPN is one of the domains in this comma separated list. Note that this may overwrite data on existing users in the database!")
        .add()
        .property()
        .name(CONFIG_KEY_GROUP_MAP)
        .label("Group map")
        .type(ProviderConfigProperty.STRING_TYPE)
        .helpText(
            "Specify the group map using a json object like this: {\"FK Group 1\": \"/Keycoak Group 1\", \"FK Group 2\": \"/Keycoak Group 2\"}, remember that group names are case sensitive!")
        .add()
        .property()
        .name(CONFIG_KEY_IMPORT_USERS_NOT_IN_MAPPED_GROUPS)
        .label("Import users not in mapped groups")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .helpText(
            "Turn this ON if you would like to also import users that are not members of any of the mapped groups.")
        .add()
        .property()
        .name(CONFIG_KEY_GROUPS_FOR_USERS_NOT_IN_MAPPED_GROUPS)
        .label("Groups for users not in mapped groups")
        .type(ProviderConfigProperty.STRING_TYPE)
        .helpText(
            "Comma separated list of Keycloak groups to be assigned to users who are not members of any of the mapped groups.")
        .add()
        .property()
        .name(
            CONFIG_KEY_ONLY_USE_GROUPS_IN_GROUP_MAP)
        .label("Only handle groups in group map")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .helpText(
            "Disregard any and all groups not mentioned above")
        .add()
        .property()
        .name(CONFIG_KEY_DO_NOT_OVERRIDE_MOBILE_WITH_EMPTY)
        .label("Do not override mobile numbers with empty value")
        .type(ProviderConfigProperty.BOOLEAN_TYPE)
        .helpText("If enabled, the mobile phone number will not be overridden if the new value is empty.")
        .add()
        .build();
  }

  @Override
  public String getId() {
    return "fk";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return configMetadata;
  }

  @Override
  public FkApiUserStorageProvider create(KeycloakSession ksession, ComponentModel model) {
    return new FkApiUserStorageProvider();
  }

  @Override
  public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId,
      UserStorageProviderModel model) {
    return syncImpl(sessionFactory, realmId, model);
  }

  @Override
  public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId,
      UserStorageProviderModel model) {
    return syncImpl(sessionFactory, realmId, model);
  }

  @Override
  public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
      throws ComponentValidationException {
    if (!config.contains(CONFIG_KEY_CVR_NUMBER)) {
      throw new ComponentValidationException("CVR number is required!");
    }

    GroupMapConfig groupMapConfig = getGroupMapConfig(session, realm, config);

    if (!groupMapConfig.errors.isEmpty() || !groupMapConfig.criticalErrors.isEmpty()) {
      List<String> allErrors = new ArrayList<>();
      allErrors.addAll(groupMapConfig.errors);
      allErrors.addAll(groupMapConfig.criticalErrors);
      throw new ComponentValidationException(
          String.format("Errors found in Group map: %s", String.join(", ", allErrors)));
    }

    // For some reason enabled is set to 't' when saving configuration.
    // This will cause provider and linked users to get disabled and subsequent
    // periodic syncs not to run,
    // so we work around that by setting enabled to "true" in the
    // validateConfiguration.
    // This was not necessary prior to version 21
    String enabled = config.getConfig().getFirst("enabled");

    if ("t".equals(enabled)) {
      logger.debug("enabled is set to 't'. Will change it to 'true' as a workaround");
      config.getConfig().put("enabled", Arrays.asList("true"));
    }
  }

  @Override
  public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
    // Periodic sync is normally only refreshed if there are changes to sync
    // intervals.
    // This means that other changes to the config is not applied to the periodic
    // sync,
    // until a restart or a change to the sync intervals.
    // So this code ensures that we refresh periodic sync upon any change to the
    // config
    if (!Objects.equals(oldModel.getConfig(), newModel.getConfig())) {
      UserStorageProviderModel oldProvider = new UserStorageProviderModel(oldModel);
      UserStorageProviderModel newProvider = new UserStorageProviderModel(newModel);

      // Only refresh periodic sync here if the intervals have not changed, otherwise
      // it would be done twice.
      // It might not do any harm, but there is no need to make Keycloak do more work
      // than necesary
      if (oldProvider.getChangedSyncPeriod() == newProvider.getChangedSyncPeriod()
          && oldProvider.getFullSyncPeriod() == newProvider.getFullSyncPeriod()) {
        logger.debug("Ensure periodic sync is refreshed if there are any changes to the config");
        StoreSyncEvent.fire(session, realm, newProvider, false);
      }
    }
  }

  private SynchronizationResult syncImpl(KeycloakSessionFactory sessionFactory, String realmId,
      UserStorageProviderModel model) {
    ClientProperties clientProperties = ClientProperties.getInstance();

    String cvrNumber = model.get(CONFIG_KEY_CVR_NUMBER);

    clientProperties.setMyndighedCvr(cvrNumber);

    String emailFieldName = model.get(CONFIG_KEY_EMAIL_FIELD_NAME, "Bruger.Adresse.email");
    String mobileFieldName = model.get(CONFIG_KEY_MOBILE_FIELD_NAME, "Bruger.Adresse.Telefon");

    FkAdminEventLogger adminEventLogger = new FkAdminEventLogger(sessionFactory, realmId);

    KeycloakSession session = sessionFactory.create();

    RealmModel realm = session.realms().getRealm(realmId);

    EmailSenderProvider emailSenderProvider = session.getProvider(EmailSenderProvider.class);

    try {
      adminEventLogger.log(String.format("user-storage/%s/sync-starting", model.getName()),
          String.format("Starting FK user synchronization for '%s'", model.getName()));
    } catch (Exception e) {
      logger.errorf(e, "FK error logging");
    }

    SynchronizationResult synchronizationResult = new SynchronizationResult();
    List<String> errors = new ArrayList<>();

    boolean hasImportFinished = false;

    try {
      GroupMapConfig groupMapConfig = getGroupMapConfig(sessionFactory, realmId, model);

      if (!groupMapConfig.criticalErrors.isEmpty()) {
        throw new ConfigException(
            String.format("Critical errors found in Group map: %s", String.join(", ", groupMapConfig.criticalErrors)));
      }

      try {
        List<SimpleUser> fkApiUsers = getFkApiUsers(groupMapConfig,
            model.get(CONFIG_KEY_IMPORT_USERS_NOT_IN_MAPPED_GROUPS, false), cvrNumber, emailFieldName, mobileFieldName);

        try {
          String allowUpdateUpnDomainsCommaSeparated = model.get(CONFIG_KEY_ALLOW_UPDATE_UPN_DOMAINS);
          List<String> allowUpdateUpnDomains = null;
          if (allowUpdateUpnDomainsCommaSeparated != null && !allowUpdateUpnDomainsCommaSeparated.isEmpty()) {
            allowUpdateUpnDomains = Arrays.stream(allowUpdateUpnDomainsCommaSeparated.split(",")).map(String::trim)
                .toList();
          }

          Boolean doNotOverrideMobileWithEmpty = model.get(CONFIG_KEY_DO_NOT_OVERRIDE_MOBILE_WITH_EMPTY, false);

          FkApiUserResult result = importApiUsers(sessionFactory, realmId, model, fkApiUsers, allowUpdateUpnDomains,
              groupMapConfig, doNotOverrideMobileWithEmpty);

          synchronizationResult = result.synchronizationResult;
          errors = result.errors;

          hasImportFinished = true;
        } catch (Exception e) {
          logger.errorf(e, "Error importing api users for federation provider '%s'!",
              model.getName());
          errors.add(
              String.format("Error importing api users for federation provider '%s'! Exception:<br/>%s",
                  model.getName(), getErrorMessage(e)));
          synchronizationResult.setFailed(1);
        }
      } catch (Exception e) {
        logger.errorf(e, "Error getting users for federation provider '%s'.", model.getName());
        errors.add(String.format(
            "Error getting users for federation provider '%s'. Exception:<br/>%s",
            model.getName(), getErrorMessage(e)));
        synchronizationResult.setFailed(1);
      }
    } catch (ConfigException e) {
      logger.errorf(e, "Configuration error for federation provider '%s': %s", model.getName(), e.getMessage());
      errors
          .add(String.format("Configuration error for federation provider '%s': %s", model.getName(), e.getMessage()));
      synchronizationResult.setFailed(1);
    } finally {
      session.close();
    }

    if (hasImportFinished) {
      adminEventLogger.log(String.format("user-storage/%s/sync-finished", model.getName()), synchronizationResult);

      if (synchronizationResult.getFailed() > 0) {
        try {
          String body = String.format(
              "Error during user synchronization for federation provider '%s' in realm: '%s'. %s users failed syncing. Errors:<br/><br/>%s",
              model.getName(), realm.getName(), synchronizationResult.getFailed(), String.join("<br/><br/>", errors));

          emailSenderProvider.send(realm.getSmtpConfig(), "log.rmgroup@f24.com", "Error in user sync", body, body);
        } catch (EmailException ex) {
          logger.errorf(ex, "Failed to send email");
        }
      }
    } else {
      adminEventLogger.log(String.format("user-storage/%s/sync-error", model.getName()),
          "See server log for more details!");

      try {
        String body = String.format(
            "Error during user synchronization for federation provider '%s' in realm: '%s'. Errors:<br/><br/>%s",
            model.getName(), realm.getName(), String.join("<br/><br/>", errors));

        emailSenderProvider.send(realm.getSmtpConfig(), "log.rmgroup@f24.com", "Error in user sync", body, body);
      } catch (EmailException ex) {
        logger.errorf(ex, "Failed to send email");
      }
    }

    return synchronizationResult;
  }

  private FkApiUserResult importApiUsers(KeycloakSessionFactory sessionFactory, final String realmId,
      final ComponentModel fedModel, List<SimpleUser> apiUsers, List<String> allowUpdateUpnDomains,
      GroupMapConfig groupMapConfig, Boolean doNotOverrideMobileWithEmpty) {
    final Map<String, GroupModel> groupMap = groupMapConfig.groupMap;
    final List<GroupModel> groupsForUsersNotInMappedGroups = groupMapConfig.groupsForUsersNotInMappedGroups;

    final String fedId = fedModel.getId();

    final Set<String> apiUsersUserNameSet = apiUsers.stream().map(u -> u.getUserName()).distinct()
        .collect(Collectors.toSet());

    final List<String> errors = new ArrayList<>();

    final AtomicInteger removedCount = new AtomicInteger(0);
    final AtomicInteger addedCount = new AtomicInteger(0);
    final AtomicInteger updatedCount = new AtomicInteger(0);
    final AtomicInteger failedCount = new AtomicInteger(0);

    final int totalExistingUsers = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory,
        (KeycloakSession session) -> {
          try {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            UserProvider userProvider = session.users();
            return userProvider.getUsersCount(realm);
          } catch (Exception e) {
            logger.errorf(e,
                "Error getting user count in federation provider '%s'. Will not be able to remove non existing users!",
                fedModel.getName());
            errors.add(String.format(
                "Error getting user count in federation provider '%s'. Will not be able to remove non existing users! Exception:<br/>%s",
                fedModel.getName(), getErrorMessage(e)));
            return -1;
          }
        });

    Boolean onlyUseGroupsInGroupMap = fedModel.get(CONFIG_KEY_ONLY_USE_GROUPS_IN_GROUP_MAP, false);

    if (totalExistingUsers > 0) {
      int totalPagesExistingUsers = (int) Math.ceil((double) totalExistingUsers / USER_REMOVE_PAGE_SIZE);

      CopyOnWriteArrayList<UserModel> usersToRemove = new CopyOnWriteArrayList<>();

      IntStream.range(0, totalPagesExistingUsers).parallel().forEach(page -> {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
          RealmModel realm = session.realms().getRealm(realmId);
          session.getContext().setRealm(realm);
          UserProvider userProvider = session.users();
          int firstResult = page * USER_REMOVE_PAGE_SIZE;
          int maxResults = USER_REMOVE_PAGE_SIZE;

          try {
            usersToRemove.addAll(userProvider
                .searchForUserStream(realm, new HashMap<>(), firstResult, maxResults)
                .filter(u -> fedId.equals(u.getFederationLink()) && !apiUsersUserNameSet.contains(u.getUsername()))
                .toList());
          } catch (Exception e) {
            logger.errorf(e,
                "Error getting users to remove in federation provider '%s'. Might not be able to remove all non existing users!",
                fedModel.getName());
            errors.add(String.format(
                "Error getting users to remove in federation provider '%s'. Might not be able to remove all non existing users! Exception:<br/>%s",
                fedModel.getName(), getErrorMessage(e)));
          }
        });
      });

      int totalUsersToRemove = usersToRemove.size();

      if (totalUsersToRemove > 0) {
        int totalPagesUsersToRemove = (int) Math.ceil((double) totalUsersToRemove / USER_REMOVE_PAGE_SIZE);
        IntStream.range(0, totalPagesUsersToRemove).parallel().forEach(page -> {

          KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            UserProvider userProvider = session.users();

            int startIndex = page * USER_REMOVE_PAGE_SIZE;
            int endIndex = Math.min(startIndex + USER_REMOVE_PAGE_SIZE, totalUsersToRemove);

            List<UserModel> usersToRemovePage = usersToRemove.subList(startIndex, endIndex);

            for (final UserModel user : usersToRemovePage) {
              try {
                userProvider.removeUser(realm, user);
                removedCount.incrementAndGet();
              } catch (Exception e) {
                logger.errorf(e,
                    "Error removing non existing user with username '%s' in federation provider '%s'",
                    user.getUsername(), fedModel.getName());
                errors.add(String.format(
                    "Error removing non existing user with username '%s' in federation provider '%s'. Exception:<br/>%s",
                    user.getUsername(), fedModel.getName(), getErrorMessage(e)));
                failedCount.incrementAndGet();
              }
            }
          });
        });
      }
    }

    int totalApiUsers = apiUsers.size();

    if (totalApiUsers > 0) {
      int totalPages = (int) Math.ceil((double) totalApiUsers / USER_IMPORT_PAGE_SIZE);
      IntStream.range(0, totalPages).parallel().forEach(page -> {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
          RealmModel realm = session.realms().getRealm(realmId);
          session.getContext().setRealm(realm);
          UserProvider userProvider = session.users();

          int startIndex = page * USER_IMPORT_PAGE_SIZE;
          int endIndex = Math.min(startIndex + USER_IMPORT_PAGE_SIZE, totalApiUsers);

          List<SimpleUser> apiUsersPage = apiUsers.subList(startIndex, endIndex);

          apiUsersPage.forEach(apiUser -> {
            try {
              UserModel importedUser;
              UserModel existingLocalUser = userProvider.getUserByUsername(realm, apiUser.getUserName());
              if (existingLocalUser == null) {
                importedUser = userProvider.addUser(realm, apiUser.getUserName());
              } else {
                if (fedId.equals(existingLocalUser.getFederationLink())) {
                  importedUser = existingLocalUser;
                } else if (allowUpdateUpnDomains != null) {
                  String upn = apiUser.getUserName();
                  if (allowUpdateUpnDomains.stream().noneMatch(domain -> upn.endsWith("@" + domain))) {
                    logger.warnf(
                        "User with userName '%s' is not updated during sync as he already exists in Keycloak database but is not linked to federation provider '%s' and UPN domain does not match any of '%s'",
                        apiUser.getUserName(), fedModel.getName(), String.join(", ", allowUpdateUpnDomains));
                    errors.add(String.format(
                        "User with userName '%s' is not updated during sync as he already exists in Keycloak database but is not linked to federation provider '%s' and UPN domain does not match any of '%s'",
                        apiUser.getUserName(), fedModel.getName(),
                        String.join(", ", allowUpdateUpnDomains)));
                    failedCount.incrementAndGet();
                    return;
                  }
                  importedUser = existingLocalUser;
                } else {
                  logger.warnf(
                      "User with userName '%s' is not updated during sync as he already exists in Keycloak database but is not linked to federation provider '%s'",
                      apiUser.getUserName(), fedModel.getName());
                  errors.add(String.format(
                      "User with userName '%s' is not updated during sync as he already exists in Keycloak database but is not linked to federation provider '%s'",
                      apiUser.getUserName(), fedModel.getName()));
                  failedCount.incrementAndGet();
                  return;
                }
              }

              boolean attributesChanged = !apiUserEqualsLocalUser(apiUser, existingLocalUser);

              if (attributesChanged) {
                importedUser.setFederationLink(fedId);
                importedUser.setEmail(apiUser.getEmail());
                importedUser.setEmailVerified(true);
                var splitName = apiUser.getPersonName().split(" ", 2);
                importedUser.setFirstName(splitName.length > 0 ? splitName[0] : "");
                importedUser.setLastName(splitName.length > 1 ? splitName[1] : "");
                String mobilePhone = apiUser.getMobile();
                if (!Strings.isNullOrEmpty(mobilePhone) || !doNotOverrideMobileWithEmpty) {
                  importedUser.setSingleAttribute("mobile", mobilePhone);
                }
                // TODO: should we check for this
                importedUser.setEnabled(true);
              }

              boolean groupsChanged = false;

              Set<String> apiUserGroups = apiUser.getGroups();

              HashSet<String> groupIds = new HashSet<>();

              HashSet<String> groupMapGroupIds = new HashSet<>();

              if (groupMap != null && !groupMap.isEmpty()) {
                for (GroupModel group : groupMap.values()) {
                  groupMapGroupIds.add(group.getId());
                }

                for (GroupModel g : groupsForUsersNotInMappedGroups) {
                  groupMapGroupIds.add(g.getId());
                }
              }

              if (groupMap != null && !groupMap.isEmpty() && apiUserGroups != null && !apiUserGroups.isEmpty()) {
                for (String apiUserGroup : apiUserGroups) {
                  if (groupMap.containsKey(apiUserGroup)) {
                    GroupModel kcGroup = groupMap.get(apiUserGroup);
                    groupIds.add(kcGroup.getId());
                    if (!importedUser.isMemberOf(kcGroup)) {
                      groupsChanged = true;
                      importedUser.joinGroup(kcGroup);
                    }
                  }
                }
                List<GroupModel> groupsToLeave = importedUser.getGroupsStream().filter(g -> {
                  if (onlyUseGroupsInGroupMap) {
                    return groupMapGroupIds.contains(g.getId()) && !groupIds.contains(g.getId());
                  } else {
                    return !groupIds.contains(g.getId());
                  }
                }).toList();

                if (!groupsToLeave.isEmpty()) {
                  groupsChanged = true;
                  groupsToLeave.forEach(importedUser::leaveGroup);
                }
              } else {
                if ((apiUserGroups == null || apiUserGroups.isEmpty())
                    && !groupsForUsersNotInMappedGroups.isEmpty()) {
                  for (GroupModel g : groupsForUsersNotInMappedGroups) {
                    groupIds.add(g.getId());
                    if (!importedUser.isMemberOf(g)) {
                      groupsChanged = true;
                      importedUser.joinGroup(g);
                    }
                  }
                  List<GroupModel> groupsToLeave = importedUser.getGroupsStream().filter(g -> {
                    if (onlyUseGroupsInGroupMap) {
                      return groupMapGroupIds.contains(g.getId()) && !groupIds.contains(g.getId());
                    } else {
                      return !groupIds.contains(g.getId());
                    }
                  }).toList();

                  if (!groupsToLeave.isEmpty()) {
                    groupsChanged = true;
                    groupsToLeave.forEach(importedUser::leaveGroup);
                  }
                } else {
                  List<GroupModel> groupsToLeave = importedUser.getGroupsStream().filter(g -> {
                    if (onlyUseGroupsInGroupMap) {
                      return groupMapGroupIds.contains(g.getId());
                    } else {
                      return true;
                    }
                  }).toList();

                  if (!groupsToLeave.isEmpty()) {
                    groupsChanged = true;
                    groupsToLeave.forEach(importedUser::leaveGroup);
                  }
                }
              }

              if (existingLocalUser == null) {
                addedCount.incrementAndGet();
              } else if (attributesChanged || groupsChanged) {
                updatedCount.incrementAndGet();
              }
            } catch (Exception e) {
              logger.errorf(e, "Failed during import of user '%s' from Fk API",
                  apiUser.getUserName());

              errors.add(String.format(
                  "Failed during import of user '%s' from Fk API. Exception:<br/>%s",
                  apiUser.getUserName(), getErrorMessage(e)));
              failedCount.incrementAndGet();
            }
          });
        });
      });
    }

    final FkSynchronizationResult syncResult = new FkSynchronizationResult();

    syncResult.setFailed(failedCount.get());
    syncResult.setAdded(addedCount.get());
    syncResult.setUpdated(updatedCount.get());
    syncResult.setRemoved(removedCount.get());
    syncResult.setFetched(totalApiUsers);

    return new FkApiUserResult(syncResult, errors);
  }

  private static boolean apiUserEqualsLocalUser(SimpleUser apiUser, UserModel existingLocalUser) {
    var splitName = apiUser.getPersonName().split(" ", 2);
    return existingLocalUser != null &&
        Objects.equals(apiUser.getUserName(), existingLocalUser.getUsername()) &&
        Objects.equals(apiUser.getEmail(), existingLocalUser.getEmail()) &&
        Objects.equals(splitName[0], existingLocalUser.getFirstName()) &&
        Objects.equals(splitName.length > 1 ? splitName[1] : "", existingLocalUser.getLastName()) &&
        Objects.equals(apiUser.getMobile(), existingLocalUser.getFirstAttribute("mobile"));
  }

  class GroupMapConfig {
    private Map<String, GroupModel> groupMap = new HashMap<>();

    private List<GroupModel> groupsForUsersNotInMappedGroups = new ArrayList<>();

    private List<String> errors = new ArrayList<>();

    private List<String> criticalErrors = new ArrayList<>();

    public Map<String, GroupModel> getGroupMap() {
      return groupMap;
    }

    public List<GroupModel> getGroupsForUsersNotInMappedGroups() {
      return groupsForUsersNotInMappedGroups;
    }

    public List<String> getErrors() {
      return errors;
    }

    public List<String> getCriticalErrors() {
      return criticalErrors;
    }

    public void setProperties(GroupMapConfig groupMapConfig) {
      groupMap = groupMapConfig.groupMap;
      groupsForUsersNotInMappedGroups = groupMapConfig.groupsForUsersNotInMappedGroups;
      errors = groupMapConfig.errors;
      criticalErrors = groupMapConfig.criticalErrors;
    }
  }

  private GroupMapConfig getGroupMapConfig(KeycloakSessionFactory sessionFactory, final String realmId,
      ComponentModel config) {
    final GroupMapConfig groupMapConfig = new GroupMapConfig();
    KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
      RealmModel realm = session.realms().getRealm(realmId);
      session.getContext().setRealm(realm);
      groupMapConfig.setProperties(getGroupMapConfig(session, realm, config));
    });
    return groupMapConfig;
  }

  private GroupMapConfig getGroupMapConfig(KeycloakSession session, RealmModel realm, ComponentModel config) {
    GroupMapConfig groupMapConfig = new GroupMapConfig();
    Map<String, GroupModel> groupMap = groupMapConfig.groupMap;
    List<GroupModel> groupsForUsersNotInMappedGroups = groupMapConfig.groupsForUsersNotInMappedGroups;
    List<String> errors = groupMapConfig.errors;
    List<String> criticalErrors = groupMapConfig.criticalErrors;

    if (config.contains(CONFIG_KEY_GROUP_MAP)) {
      String json = config.get(CONFIG_KEY_GROUP_MAP);

      try {
        Map<String, Object> jsonMap = new JSONObject(json).toMap();
        jsonMap.forEach((k, v) -> {
          try {
            GroupModel kcGroup = KeycloakModelUtils.findGroupByPath(session, realm, v.toString());
            if (kcGroup != null) {
              groupMap.put(k, kcGroup);
            } else {
              String errorMessage = String.format("Keycloak group '%s' not found.", v);
              logger.error(errorMessage);
              errors.add(errorMessage);
            }
          } catch (Exception e) {
            String errorMessage = String.format("Error getting Keycloak group '%s'. '%s'", v, e.getMessage());
            logger.error(errorMessage, e);
            criticalErrors.add(errorMessage);
          }
        });
      } catch (JSONException e) {
        String errorMessage = String.format("Error in group map JSON '%s'. '%s'", json, e.getMessage());
        logger.error(errorMessage, e);
        criticalErrors.add(errorMessage);
      }
    }

    if (config.contains(CONFIG_KEY_GROUPS_FOR_USERS_NOT_IN_MAPPED_GROUPS)
        && config.contains(CONFIG_KEY_IMPORT_USERS_NOT_IN_MAPPED_GROUPS)) {
      Arrays.stream(config.get(CONFIG_KEY_GROUPS_FOR_USERS_NOT_IN_MAPPED_GROUPS).split(",")).map(String::trim)
          .forEach(g -> {
            try {
              GroupModel kcGroup = KeycloakModelUtils.findGroupByPath(session, realm, g);
              if (kcGroup != null) {
                groupsForUsersNotInMappedGroups.add(kcGroup);
              } else {
                String errorMessage = String.format("Keycloak group '%s' not found.", g);
                logger.error(errorMessage);
                errors.add(errorMessage);
              }
            } catch (Exception e) {
              String errorMessage = String.format("Error getting Keycloak group '%s'. '%s'", g, e.getMessage());
              logger.error(errorMessage, e);
              criticalErrors.add(errorMessage);
            }
          });
    }

    return groupMapConfig;
  }

  private static List<SimpleUser> getFkApiUsers(GroupMapConfig groupMapConfig,
      boolean importUsersNotInMappedGroups, String cvrNumber, String emailFieldName, String mobileFieldName)
      throws Exception {
    final Map<String, GroupModel> groupMap = groupMapConfig.groupMap;
    final Map<String, SimpleUser> usersMap = new HashMap<>();

    var userFetcherService = new UserFetcherService(cvrNumber, emailFieldName, mobileFieldName);

    if (groupMap != null && !groupMap.isEmpty()) {
      Map<String, String> groupIdsAndMapKeys = userFetcherService.getUnitMap(new ArrayList<>(groupMap.keySet()));

      groupIdsAndMapKeys.forEach((groupId, groupMapKey) -> {
        try {
          List<SimpleUser> fkApiUsers = userFetcherService.fetchUsersInUnitUuids(List.of(groupId));

          fkApiUsers.forEach(user -> {
            if (!user.getGroups().contains(groupMapKey)) {
              user.addGroup(groupMapKey);
            }

            usersMap.put(user.getUserName(), user);
          });

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }

    if (importUsersNotInMappedGroups) {
      List<SimpleUser> users = userFetcherService.fetchAllUsers().stream()
          .filter(u -> {
            return !usersMap.containsKey(u.getUserName());
          }).toList();

      users.forEach(u -> {
        usersMap.put(u.getUserName(), u);
      });
    }

    return usersMap.values().stream().toList();
  }

  private String getErrorMessage(Throwable e) {
    String errorMessage = e.getMessage();
    Throwable cause = e.getCause();

    if (cause != null) {
      errorMessage += "<br/>Caused by: " + getErrorMessage(cause);
    }

    return errorMessage;
  }
}