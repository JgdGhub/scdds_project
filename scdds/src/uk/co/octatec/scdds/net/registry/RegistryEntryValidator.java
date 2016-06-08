package uk.co.octatec.scdds.net.registry;

import java.util.List;

/**
 * Created by Jeromy Drake on 01/05/2016.
 */
public interface RegistryEntryValidator {
    interface Validatable {

        String getHost();
        int getPort();
        String getCacheName();
        String getGroup();
        void setInvalid();
        void setConnectionCount(int count);
        int getConnectionCount();

    }

    void validate(Validatable instance);
}
