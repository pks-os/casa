package org.gluu.casa.plugins.helloworld;

import org.gluu.casa.misc.Utils;
import org.gluu.casa.service.IPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;

/**
 * A ZK <a href="http://books.zkoss.org/zk-mvvm-book/8.0/viewmodel/index.html" target="_blank">ViewModel</a> that acts
 * as the "controller" of page <code>index.zul</code> in this sample plugin. See <code>viewModel</code> attribute of
 * panel component of <code>index.zul</code>.
 * @author jgomer
 */
public class HelloWorldVM {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String message;
    private String organizationName;
    private IPersistenceService persistenceService;

    /**
     * Getter of private class field <code>organizationName</code>.
     * @return A string with the value of the organization name found in your Gluu installation. Find this value in
     * Gluu Server oxTrust GUI at "Configuration" &gt; "Organization configuration"
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Getter of private class field <code>message</code>.
     * @return A string value
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter of private class field <code>message</code>.
     * @param message A string with the contents typed in text box of page index.zul
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Initialization method for this ViewModel.
     */
    @Init
    public void init() {
        logger.info("Hello World ViewModel inited");
        persistenceService = Utils.managedBean(IPersistenceService.class);
    }

    /**
     * The method called when the button on page <code>index.zul</code> is pressed. It sets the value for
     * <code>organizationName</code>.
     */
    @NotifyChange("organizationName")
    @Command
    public void loadOrgName() {
        logger.debug("You typed {}", message);
        organizationName = persistenceService.getOrganization().getDisplayName();
    }

}
