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

import static org.restcomm.imscf.el.stack.LwcommMessageReceiver.LWC_MESSAGE_PATTERN;
import org.restcomm.imscf.el.call.IMSCFCall;
import org.restcomm.imscf.el.call.CallFactoryBean;
import org.restcomm.imscf.el.call.CallStore;
import org.restcomm.imscf.el.call.history.ElEventCreator;
import org.restcomm.imscf.el.config.ConfigBean;
import org.restcomm.imscf.el.sip.adapters.SipApplicationSessionAdapter;
import org.restcomm.imscf.el.sip.servlets.SipServletResources;
import org.restcomm.imscf.el.stack.CallContext.ContextLayer;
import org.restcomm.imscf.el.tcap.call.TCAPCall;
import org.restcomm.imscf.common.LwcTags;
import org.restcomm.imscf.common.SLELRouter;
import org.restcomm.imscf.common.SccpDialogId;
import org.restcomm.imscf.common.SccpSerializer;
import org.restcomm.imscf.common.TcapDialogId;
import org.restcomm.imscf.common.util.TCAPMessageInfo;
import org.restcomm.imscf.common.util.TCAPMessageInfo.MessageType;
import org.restcomm.imscf.common.util.ImscfCallId;
import org.restcomm.imscf.common.lwcomm.service.IncomingTextMessage;
import org.restcomm.imscf.common.lwcomm.service.LwCommServiceProvider;
import org.restcomm.imscf.common.lwcomm.service.MessageReceiver;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.SendResult.Type;
import org.restcomm.imscf.common.lwcomm.service.SendResultFuture;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

import javax.servlet.sip.SipApplicationSession;

import javolution.util.FastMap;

import org.mobicents.protocols.ss7.sccp.SccpListener;
import org.mobicents.protocols.ss7.sccp.SccpManagementEventListener;
import org.mobicents.protocols.ss7.sccp.SccpProvider;
import org.mobicents.protocols.ss7.sccp.NetworkIdState;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;
import org.mobicents.protocols.ss7.sccp.impl.message.MessageFactoryImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.message.MessageFactory;
import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.ss7.congestion.ExecutorCongestionMonitor;
import org.mobicents.servlet.sip.core.session.MobicentsSipApplicationSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * Implementation of an "SCCP User Adaptation" like layer to be used underneath the jss7 TCAP stack.
 * SCCP messages to/from the TCAP stack are communicated to the SL via lwcomm.
 */
public class SUAImpl implements MessageReceiver, SccpProvider {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SUAImpl.class);

    // stack is not really used, only by messagefactory (stack.newSls()).
    // also passed to SCCP messages that just ignore it.
    private final MessageFactory mf = new MessageFactoryImpl(new SccpStackImpl("VSCCP"));
    private final ParameterFactory pf = new ParameterFactoryImpl();
    private final ConcurrentHashMap<Integer, SccpListener> sccpListeners = new ConcurrentHashMap<>();

    private transient SLELRouter<SlElMappingData> slRouter;
    private transient CallStore callStore;
    private transient ConfigBean configBean;
    private transient CallFactoryBean callFactoryBean;

    public void setSlRouter(SLELRouter<SlElMappingData> slRouter) {
        this.slRouter = slRouter;
    }

    public void setCallStore(CallStore callStore) {
        this.callStore = callStore;
    }

    public void setConfigBean(ConfigBean configBean) {
        this.configBean = configBean;
    }

    public void setCallFactoryBean(CallFactoryBean callFactoryBean) {
        this.callFactoryBean = callFactoryBean;
    }

    private TCAPMessageInfo updateSLMappingForIncomingMessage(IncomingTextMessage lwcmsg, SccpDataMessage msg) {
        TCAPMessageInfo info = TCAPMessageInfo.parse(msg.getData());
        logger.debug("Incoming TCAP info: " + info.toString());
        SccpDialogId sdid = SccpDialogId.extractFromSccpMessage(msg, true);
        TcapDialogId tdid = TcapDialogId.extractFromTCAPMessageInfo(info, true);

        SlElMappingData mappingData = new SlElMappingData();
        mappingData.setNodeName(lwcmsg.getFrom().getName());
        ImscfCallId imscfCallId = ImscfCallId.parse(lwcmsg.getGroupId());
        mappingData.setImscfCallId(imscfCallId);
        try (IMSCFCall call = callStore.getCallByImscfCallId(imscfCallId.toString())) {
            if (call != null) {
                call.getCallHistory().addEvent("->LWC(" + info.getMessageType() + ", " + lwcmsg.getId() + ")");
            }
        }

        switch (info.getMessageType()) {
        case TC_BEGIN:
        case TC_CONTINUE:
            slRouter.setMappingData(sdid, tdid, mappingData);
            break;
        case TC_ABORT:
        case TC_END:
            slRouter.setMappingData(sdid, tdid, null);
            break;
        default:
            throw new AssertionError(info.getMessageType());
        }
        return info;
    }

    @Override
    public void onMessage(IncomingTextMessage lwcommMessage) {
        MDC.clear();
        String payload = lwcommMessage.getPayload();
        Matcher m = LWC_MESSAGE_PATTERN.matcher(payload);
        String content, data;
        if (m.matches()) {
            // group 1 is the target, i.e. this listener
            content = m.group(2);
            data = m.group(3);
            switch (content) { // NOPMD ignore switch with less than 2 branches check
            case "SccpDataMessage":

                SccpDataMessage sdm = SccpSerializer.deserialize(data);

                TCAPMessageInfo info = updateSLMappingForIncomingMessage(lwcommMessage, sdm);

                int ssn = sdm.getCalledPartyAddress().getSubsystemNumber();
                SccpListener listener = sccpListeners.get(ssn);
                if (listener != null) {
                    String imscfCallId = lwcommMessage.getGroupId();
                    // Create if it doesn't exist, but only for TC_BEGIN - in other cases, the SAS must already be
                    // present. If it isn't, it could be due to a CONTINUE/END received for an already deleted call and
                    // the SAS should not be recreated. The TCAP stack will simply respond with a P_ABORT / missing
                    // dialog.
                    SipApplicationSession sas = SipServletResources.getSipSessionsUtil().getApplicationSessionByKey(
                            imscfCallId, info.getMessageType() == MessageType.TC_BEGIN);
                    assert sas == null
                            || sas instanceof SipApplicationSessionAdapter
                            && ((SipApplicationSessionAdapter) sas).getDelegate() instanceof MobicentsSipApplicationSession;

                    if (sas != null) {
                        // lock on appsession here, before TCAP stack locks on TCAP dialog
                        logger.trace("Trying to lock on appsession for call {}", imscfCallId);
                        ((MobicentsSipApplicationSession) ((SipApplicationSessionAdapter) sas).getDelegate()).acquire();
                        logger.trace("Lock acquired");
                    } else {
                        logger.trace("No appsession to lock on for {} in call {}", info.getMessageType(), imscfCallId);
                    }

                    // call id is used later by cap/map listener when creating the actual call
                    try (ContextLayer cl = CallContext.with(imscfCallId, callStore, configBean, callFactoryBean)) {
                        listener.onMessage(sdm);
                    } finally {
                        if (sas != null) {
                            logger.trace("Trying to unlock appsession for call {}", imscfCallId);
                            ((MobicentsSipApplicationSession) ((SipApplicationSessionAdapter) sas).getDelegate())
                                    .release();
                            logger.trace("Lock released");
                        }
                    }
                } else {
                    logger.error("No SCCP listener for SSN: {}!", ssn);
                }
                break;
            default:
                logger.error("Unknown content: " + content);
                break;
            }
        } else {
            logger.error("Message from SL not understood.");
        }
    }

    @Override
    public void deregisterManagementEventListener(SccpManagementEventListener arg0) {
        throw new RuntimeException("Operation not allowed");
    }

    @Override
    public void deregisterSccpListener(int ssn) {
        sccpListeners.remove(ssn);
    }

    @Override
    public int getMaxUserDataLength(SccpAddress arg0, SccpAddress arg1, int msgNetworkId) {
        // TODO FIXME
        return 1000;
    }

    @Override
    public MessageFactory getMessageFactory() {
        return mf;
    }

    @Override
    public ParameterFactory getParameterFactory() {
        return pf;
    }

    @Override
    public void registerManagementEventListener(SccpManagementEventListener arg0) {
        throw new RuntimeException("Operation not allowed");
    }

    @Override
    public void registerSccpListener(int ssn, SccpListener listener) {
        sccpListeners.put(ssn, listener);
    }

    @Override
    public void send(SccpDataMessage sdm) throws IOException {

        TCAPMessageInfo info = TCAPMessageInfo.parse(sdm.getData());
        logger.debug("Outgoing TCAP info: " + info.toString());
        SccpDialogId sdid = SccpDialogId.extractFromSccpMessage(sdm, false);
        TcapDialogId tdid = TcapDialogId.extractFromTCAPMessageInfo(info, false);

        String lwcommRouteName;
        String lwcTag;
        ImscfCallId callid;

        switch (info.getMessageType()) {
        case TC_BEGIN:
            // choose arbitrary SL node
            lwcommRouteName = slRouter.getRouteToAnyNode().getName();
            lwcTag = LwcTags.NEW_SESSION;
            // TODO: retrieve the callid of the actual call that we should already be locked on instead of this...
            try (TCAPCall call = callStore.getCallByLocalTcapTrId(tdid.getLocalTcapTID())) {
                callid = ImscfCallId.parse(call.getImscfCallId());
            }
            break;
        case TC_CONTINUE:
        case TC_END:
        case TC_ABORT:
            // if the dtid was not saved yet (first continue / dialog accept), this will store that as well.
            SlElMappingData mappingData = slRouter.getMappingData(sdid, tdid);
            if (mappingData == null) {
                logger.warn("Missing SL node mapping entry for {} / {} {}, dropping message!", sdid, tdid,
                        info.getMessageType());
                return;
            }
            lwcommRouteName = slRouter.getDirectRouteNameTo(mappingData.getNodeName());
            lwcTag = LwcTags.IN_SESSION;
            callid = mappingData.getImscfCallId();

            if (info.getMessageType() != MessageType.TC_CONTINUE) {
                slRouter.setMappingData(sdid, tdid, null);
            }
            break;
        default:
            throw new AssertionError(info.getMessageType());
        }

        String payload = "Target: SccpProvider\r\n" + "Content: SccpDataMessage\r\n" + "\r\n"
                + SccpSerializer.serialize(sdm);

        logger.debug("Sending {} / {} {} to SL on route: [{}], payload:\n{}", sdid, tdid, info.getMessageType(),
                lwcommRouteName, payload);

        SendResultFuture<SendResult> result = LwCommServiceProvider.getService().send(lwcommRouteName,
                TextMessage.builder(payload).setGroupId(callid.toString()).setTag(lwcTag).create());
        // for now we wait for the sending to complete here
        try {
            SendResult sr = result.get();
            if (sr.getType() == Type.SUCCESS) {
                ElEventCreator.addEventByImscfCallId(callid.toString(), "<-LWC_OK(" + info.getMessageType() + ", "
                        + result.getMessageId() + ")");
                String name = sr.getActualDestination().getName();
                logger.debug("Message sent to SL: {}", sr);
                if (info.getMessageType() == MessageType.TC_BEGIN) {
                    SlElMappingData mappingData = new SlElMappingData();
                    mappingData.setNodeName(name);
                    mappingData.setImscfCallId(callid);
                    slRouter.setMappingData(sdid, tdid, mappingData);
                }
            } else {
                logger.warn("Failed to send message to SL node! {} / {} -> {}, result: {}", sdid, tdid,
                        lwcommRouteName, sr);
                ElEventCreator.addEventByImscfCallId(callid.toString(), "<-LWC_ERROR(" + info.getMessageType() + ", "
                        + result.getMessageId() + ")");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error getting SendResult", e);
        }
    }

    @Override
    public void coordRequest(int ssn) {
        logger.error("Unimplemented method is called: coordRequest()");
	}

    @Override
    public FastMap<Integer, NetworkIdState> getNetworkIdStateList() {
        logger.error("Unimplemented method is called: getNetworkIdStateList()");
	    return null;
	}

    @Override
    public ExecutorCongestionMonitor[] getExecutorCongestionMonitorList() {
        logger.error("Unimplemented method is called: getExecutorCongestionMonitorList()");
	    return null;
	}

}
