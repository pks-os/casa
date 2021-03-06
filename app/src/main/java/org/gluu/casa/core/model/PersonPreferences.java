package org.gluu.casa.core.model;

import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

@DataEntry
@ObjectClass(values = { "top", "gluuPerson" })
public class PersonPreferences extends BasePerson {

    @AttributeName(name = "oxPreferredMethod")
    private String preferredMethod;

    @AttributeName(name = "oxStrongAuthPolicy")
    private String strongAuthPolicy;

    @AttributeName(name = "oxTrustedDevicesInfo")
    private String trustedDevices;

    public String getPreferredMethod() {
        return preferredMethod;
    }

    public String getStrongAuthPolicy() {
        return strongAuthPolicy;
    }

    public String getTrustedDevices() {
        return trustedDevices;
    }

    public void setPreferredMethod(String preferredMethod) {
        this.preferredMethod = preferredMethod;
    }

    public void setStrongAuthPolicy(String strongAuthPolicy) {
        this.strongAuthPolicy = strongAuthPolicy;
    }

    public void setTrustedDevices(String trustedDevicesInfo) {
        this.trustedDevices = trustedDevicesInfo;
    }

}
