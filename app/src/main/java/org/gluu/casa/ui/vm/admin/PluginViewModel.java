package org.gluu.casa.ui.vm.admin;

import org.gluu.casa.core.ExtensionsManager;
import org.gluu.casa.extension.AuthnMethod;
import org.gluu.casa.timer.FSPluginChecker;
import org.gluu.casa.ui.UIUtils;
import org.gluu.casa.misc.Utils;
import org.gluu.casa.ui.model.PluginData;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zkplus.cdi.DelegatingVariableResolver;
import org.zkoss.zul.Messagebox;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * @author jgomer
 */
@VariableResolver(DelegatingVariableResolver.class)
public class PluginViewModel extends MainViewModel {

    private static final Class<AuthnMethod> AUTHN_METHOD = AuthnMethod.class;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("extensionsManager")
    private ExtensionsManager extManager;

    @WireVariable("fSPluginChecker")
    private FSPluginChecker fspchecker;

    private List<PluginData> pluginList;

    private PluginData pluginToShow;

    private boolean engineAvailable;

    public List<PluginData> getPluginList() {
        return pluginList;
    }

    public PluginData getPluginToShow() {
        return pluginToShow;
    }

    public boolean isEngineAvailable() {
        return engineAvailable;
    }

    @Init
    public void init() {
        pluginList = new ArrayList<>();
        engineAvailable = extManager.getPluginsRoot() != null;
        extManager.getPlugins().forEach(wrapper -> pluginList.add(buildPluginData(wrapper)));
    }

    @NotifyChange({"pluginToShow"})
    @Command
    public void showPlugin(@BindingParam("id") String pluginId) {
        pluginToShow = pluginList.stream().filter(pl -> pl.getDescriptor().getPluginId().equals(pluginId)).findAny().orElse(null);
    }

    @NotifyChange({"pluginToShow"})
    @Command
    public void uploaded(@BindingParam("uplEvent") UploadEvent evt) {

        try {
            pluginToShow = null;
            byte[] blob = evt.getMedia().getByteData();
            logger.debug("Size of blob received: {} bytes", blob.length);

            try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(blob), false)) {

                Manifest m = jis.getManifest();
                if (m != null) {
                    String id = m.getMainAttributes().getValue("Plugin-Id");
                    String version = m.getMainAttributes().getValue("Plugin-Version");
                    String deps = m.getMainAttributes().getValue("Plugin-Dependencies");

                    if (pluginList.stream().anyMatch(pl -> pl.getDescriptor().getPluginId().equals(id))) {
                        UIUtils.showMessageUI(false, Labels.getLabel("adm.plugins_already_existing", new String[] { id }));
                    } else if (Stream.of(id, version).allMatch(Utils::isNotEmpty)) {
                        try {
                            if (Utils.isNotEmpty(deps)) {
                                logger.warn("This plugin reports dependencies. This feature is not available in Gluu Casa");
                                logger.warn("Your plugin may not work properly");
                            }
                            //Copy the jar to plugins dir
                            Files.write(Paths.get(extManager.getPluginsRoot().toString(), evt.getMedia().getName()), blob, StandardOpenOption.CREATE_NEW);
                            logger.info("Plugin jar file copied to app plugins directory");
                            Messagebox.show(Labels.getLabel("adm.plugins_deploy_pending"));
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                            UIUtils.showMessageUI(false);
                        }
                    } else {
                        UIUtils.showMessageUI(false, Labels.getLabel("adm.plugins_invalid_plugin"));
                        logger.error("Plugin's manifest file missing ID and/or Version");
                    }

                } else {
                    UIUtils.showMessageUI(false, Labels.getLabel("adm.plugins_invalid_plugin"));
                    logger.error("Jar file with no manifest file");
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Command
    public void deletePlugin(@BindingParam("id") String pluginId, @BindingParam("provider") String provider) {

        if (getSettings().getAcrPluginMap().values().contains(pluginId)) {
            Messagebox.show(Labels.getLabel("adm.plugin_plugin_bound_method"), null, Messagebox.OK, Messagebox.EXCLAMATION);
        } else {
            logger.info("Attempting to remove plugin {}", pluginId);
            provider = Utils.isEmpty(provider) ? Labels.getLabel("adm.plugins_nodata") : provider;
            String msg = Labels.getLabel("adm.plugins_confirm_del", new String[]{ pluginId, provider });

            Messagebox.show(msg, null, Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                    event -> {
                        if (Messagebox.ON_YES.equals(event.getName())) {
                            if (fspchecker.removePluginFile(pluginId)) {
                                Messagebox.show(Labels.getLabel("adm.plugins_undeploy_pending"));
                            } else {
                                Messagebox.show(Labels.getLabel("adm.plugins_removal_failed"));
                            }
                            pluginToShow = null;
                            BindUtils.postNotifyChange(null, null, PluginViewModel.this, "pluginToShow");
                        }
                    }
            );
        }

    }


    @NotifyChange({"pluginToShow"})
    @Command
    public void hidePluginDetails() {
        pluginToShow = null;
    }

    private PluginData buildPluginData(PluginWrapper pw) {

        PluginDescriptor pluginDescriptor = pw.getDescriptor();
        logger.debug("Building a PluginData instance for plugin {}", pw.getPluginId());
        PluginData pl = new PluginData();

        PluginState plState = pw.getPluginState();
        //In practice resolved (that is, just loaded not started) could be seen as stopped
        plState = plState.equals(PluginState.RESOLVED) ? PluginState.STOPPED : plState;

        pl.setState(Labels.getLabel("adm.plugins_state." + plState.toString()));
        pl.setPath(pw.getPluginPath().toString());
        pl.setDescriptor(pluginDescriptor);

        if (PluginState.STARTED.equals(plState)) {
            //pf4j doesn't give any info if not in started state
            pl.setExtensions(buildExtensionList(pw));
        }

        return pl;

    }

    private List<String> buildExtensionList(PluginWrapper wrapper) {

        List<String> extList = new ArrayList<>();
        PluginManager manager = wrapper.getPluginManager();
        String pluginId = wrapper.getPluginId();
        logger.trace("Building human-readable extensions list for plugin {}", pluginId);

        //plugin manager's getExtension methods outputs data only when the plugin is already started! (not simply loaded)
        for (Object obj : manager.getExtensions(pluginId)) {
            Class cls = obj.getClass();

            if (!AUTHN_METHOD.isAssignableFrom(cls)) {
                extList.add(getExtensionLabel(
                        Stream.of(cls.getInterfaces()).findFirst().map(Class::getName).orElse(""),
                        cls.getSimpleName()));
            }
        }

        for (AuthnMethod method : manager.getExtensions(AUTHN_METHOD, pluginId)) {
            String text = Labels.getLabel(method.getUINameKey());
            String acr = method.getAcr();

            if (Optional.ofNullable(getSettings().getAcrPluginMap().get(acr)).map(id -> pluginId.equals(id)).orElse(false)) {
                text += Labels.getLabel("adm.plugins_acr_handler", new String[]{ acr });
            }
            extList.add(getExtensionLabel(AUTHN_METHOD.getName(), text));
        }

        return extList;

    }

    private String getExtensionLabel(String clsName, Object ...args) {
        String text = Labels.getLabel("adm.plugins_extension." + clsName, args);
        return text == null ? clsName.substring(clsName.lastIndexOf(".") + 1) : text;
    }

}
