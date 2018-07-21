/*
 * cred-manager is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package org.gluu.credmanager.timer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.ldap.sdk.Filter;
import org.gluu.credmanager.conf.TrustedDevicesSettings;
import org.gluu.credmanager.conf.sndfactor.TrustedDevice;
import org.gluu.credmanager.conf.sndfactor.TrustedOrigin;
import org.gluu.credmanager.core.LdapService;
import org.gluu.credmanager.core.TimerService;
import org.gluu.credmanager.core.ldap.PersonPreferences;
import org.quartz.JobExecutionContext;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author jgomer
 */
@Named
@ApplicationScoped
public class TrustedDevicesSweeper extends JobListenerSupport {

    private static final int TRUSTED_DEVICE_EXPIRATION_DAYS = 30;
    private static final int TRUSTED_LOCATION_EXPIRATION_DAYS = 15;

    @Inject
    private Logger logger;

    @Inject
    private TimerService timerService;

    @Inject
    private LdapService ldapService;

    private String quartzJobName;
    private long locationExpiration;
    private long deviceExpiration;
    private ObjectMapper mapper;

    @PostConstruct
    private void inited() {
        mapper = new ObjectMapper();
        quartzJobName = getClass().getSimpleName() + "_sweep";
    }

    public long getLocationExpirationDays() {
        return TimeUnit.MILLISECONDS.toDays(locationExpiration);
    }

    public long getDeviceExpirationDays() {
        return TimeUnit.MILLISECONDS.toDays(deviceExpiration);
    }

    public void setup(TrustedDevicesSettings tsettings) {
        locationExpiration = TimeUnit.DAYS.toMillis(Optional.ofNullable(tsettings)
                .map(TrustedDevicesSettings::getLocationExpirationDays).orElse(TRUSTED_LOCATION_EXPIRATION_DAYS));
        deviceExpiration = TimeUnit.DAYS.toMillis(Optional.ofNullable(tsettings)
                .map(TrustedDevicesSettings::getDeviceExpirationDays).orElse(TRUSTED_DEVICE_EXPIRATION_DAYS));
    }

    public void activate() {
        try {
            int oneDay = (int) TimeUnit.DAYS.toSeconds(1);
            timerService.addListener(this, quartzJobName);
            //Start in one second and repeat indefinitely once every day
            timerService.schedule(quartzJobName, 1, -1, oneDay);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    @Override
    public String getName() {
        return quartzJobName;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

        logger.info("TrustedDevicesSweeper. Running timer job");
        long now = System.currentTimeMillis();
        List<PersonPreferences> people = getPeopleTrustedDevices();

        for (PersonPreferences person : people) {
            String jsonStr = null;
            try {
                String trustedDevicesInfo = ldapService.getDecryptedString(person.getTrustedDevicesInfo());
                List<TrustedDevice> list = mapper.readValue(trustedDevicesInfo, new TypeReference<List<TrustedDevice>>() { });
                //logger.debug("List is {}",mapper.writeValueAsString(list));

                if (removeExpiredData(list, now)) {
                    //update list
                    jsonStr = mapper.writeValueAsString(list);
                    updateTrustedDevices(person, ldapService.getEncryptedString(jsonStr));
                }
            } catch (Exception e) {
                if (jsonStr == null) {
                    //This may happen when data in oxTrustedDevicesInfo attribute could not be parsed (e.g. migration
                    //of gluu version brought change in encryption salt?)
                    updateTrustedDevices(person, null);
                }
                logger.error(e.getMessage(), e);
            }
        }

    }

    private boolean removeExpiredData(List<TrustedDevice> list, long time) {

        boolean changed = false;
        List<Integer> deviceIndexes = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            TrustedDevice device = list.get(i);
            List<TrustedOrigin> origins = device.getOrigins();

            if (origins != null) {
                List<Integer> origIndexes = new ArrayList<>();
                for (int j = 0; j < origins.size(); j++) {
                    long timeStamp = origins.get(j).getTimestamp();

                    if (time - timeStamp > locationExpiration) {
                        origIndexes.add(0, j);
                    }
                }
                //Remove expired ones from the origins. This is a right-to-left removal
                origIndexes.forEach(ind -> origins.remove(ind.intValue()));    //intValue() is important here!
                changed = origIndexes.size() > 0;
            }
            if (device.getAddedOn() > 0 && time - device.getAddedOn() > deviceExpiration) {
                deviceIndexes.add(0, i);
                changed = true;
            }
        }
        //Right-to-left removal of expired devices
        deviceIndexes.forEach(ind -> list.remove(ind.intValue()));
        return changed;

    }

    private List<PersonPreferences> getPeopleTrustedDevices() {

        List<PersonPreferences> list = new ArrayList<>();
        try {
            String filther = Filter.createPresenceFilter("oxTrustedDevicesInfo").toString();
            list = ldapService.find(PersonPreferences.class, ldapService.getPeopleDn(), filther);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return list;

    }

    private void updateTrustedDevices(PersonPreferences person, String value) {

        String uid = person.getUid();
        logger.trace("TrustedDevicesSweeper. Cleaning expired trusted devices for user '{}'", uid);
        person.setTrustedDevices(value);
        ldapService.modify(person, PersonPreferences.class);

    }


}