/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.imscf.el.stack;

import org.restcomm.imscf.common.lwcomm.service.IncomingTextMessage;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;
import org.restcomm.imscf.el.call.MDCParameters;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Shared LwComm listener for the EL stack that provides routing of LwComm messages between EL modules.
 */
public class LwcommMessageReceiver implements LwCommMessageProvider, MessageReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(LwcommMessageReceiver.class);
    public static final Pattern LWC_MESSAGE_PATTERN = Pattern.compile("^Target: (.*)\r\nContent: (.*)\r\n\r\n(.*)$");

    private ConcurrentHashMap<String, MessageReceiver> modules = new ConcurrentHashMap<>();

    @Override
    public void addModuleListener(String targetName, MessageReceiver targetModule) {
        modules.put(targetName, targetModule);
        LOG.trace("Added module {} to LwComm target list.", targetName);
    }

    @Override
    public void onMessage(IncomingTextMessage lwcommMessage) {
        MDC.clear();
        try {
            // Start logging with the imscfCallId (which is the same as the Group-Id) immediately, even without locking on
            // the call. If a call is found, the value will be overwritten and other parameters added as well.
            Optional.ofNullable(lwcommMessage.getGroupId()).ifPresent(
                    gid -> MDC.put(MDCParameters.IMSCF_CALLID.getKey(), gid));
            LOG.debug("Message from SL:\n " + lwcommMessage);
            Matcher m = LWC_MESSAGE_PATTERN.matcher(lwcommMessage.getPayload());
            String target;
            if (m.matches()) {
                target = m.group(1);
                MessageReceiver module = modules.get(target);
                if (module != null) {
                    LOG.trace("Delivering to module listener: {}", target);
                    // TODO: pass some kind of parsed message to the module to avoid double parsing
                    module.onMessage(lwcommMessage);
                } else {
                    LOG.warn("Unknown EL target: {}", target);
                }
            } else {
                LOG.error("Message from SL not understood.");
            }
        } finally {
            MDC.clear();
        }
    }

}
