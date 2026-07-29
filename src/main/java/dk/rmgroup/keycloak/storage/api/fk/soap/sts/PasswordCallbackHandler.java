package dk.rmgroup.keycloak.storage.api.fk.soap.sts;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import dk.rmgroup.keycloak.storage.api.fk.soap.utils.ClientProperties;

/**
 * This class is a standard way of providing password to keystores in CXF.
 */
public class PasswordCallbackHandler implements CallbackHandler {

    private String password;

    @Override
    public void handle(final Callback[] callbacks) {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback wsPasswordCallback = (WSPasswordCallback) callback;
                if (wsPasswordCallback.getUsage() == WSPasswordCallback.DECRYPT || wsPasswordCallback.getUsage() == WSPasswordCallback.SIGNATURE) {
                    wsPasswordCallback.setPassword(getPassword());
                }
            }
        }
    }

    private String getPassword() {
        if (password == null) {
            password = ClientProperties.getInstance().getKeystorePassword();
        }

        return password;
    }
}
