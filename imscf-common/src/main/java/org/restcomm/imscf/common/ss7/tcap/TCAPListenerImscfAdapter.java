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
package org.restcomm.imscf.common.ss7.tcap;

import org.restcomm.imscf.common.config.ApplicationContextType;
import org.restcomm.imscf.common.util.overload.OverloadProtector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;
import org.mobicents.protocols.ss7.tcap.api.NamedTCListener;
import org.mobicents.protocols.ss7.tcap.api.TCAPProvider;
import org.mobicents.protocols.ss7.tcap.api.TCAPSendException;
import org.mobicents.protocols.ss7.tcap.api.TCListener;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.Dialog;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCBeginIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCContinueIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCEndIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCNoticeIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCPAbortIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUniIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUserAbortIndication;
import org.mobicents.protocols.ss7.tcap.api.tc.dialog.events.TCUserAbortRequest;
import org.mobicents.protocols.ss7.tcap.asn.ApplicationContextName;
import org.mobicents.protocols.ss7.tcap.asn.InvokeImpl;
import org.mobicents.protocols.ss7.tcap.asn.comp.Invoke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCListener implementaion which routes callback to the appropriate stack.
 *
 * @author Balogh Gabor
 *
 */
public class TCAPListenerImscfAdapter implements TCListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCAPListenerImscfAdapter.class.toString());

    private NamedTCListener capListener;
    private NamedTCListener mapListener;
    private List<TCListener> tcListeners = new CopyOnWriteArrayList<TCListener>();
    private TCAPProvider tcapProvider;

    public TCAPListenerImscfAdapter(TCAPProvider tcapProvider) {
        super();
        this.tcapProvider = tcapProvider;
    }

    private NamedTCListener findProperListener(Dialog d) {
        NamedTCListener ret = null;
        ApplicationContextName acn = d.getApplicationContextName();
        ApplicationContextType act = ImscfTCAPUtil.getApplicationContext(acn.getOid());
        LOGGER.debug("findProperListener: acn={} act={}", acn, act);

        if (ImscfTCAPUtil.isCapMessage(act)) {
            ret = capListener;
        } else if (ImscfTCAPUtil.isMapMessage(act)) {
            ret = mapListener;
        }
        return ret;
    }

    public void removeTCListener(TCListener lst) {
        if (tcListeners.contains(lst)) {
            tcListeners.remove(lst);
            return;
        }
        if (!(lst instanceof NamedTCListener)) {
            return;
        }
        String name = ((NamedTCListener) lst).getName();
        if (capListener != null && capListener.getName().equals(name)) {
            capListener = null;
        } else if (mapListener != null && mapListener.equals(name)) {
            mapListener = null;
        }
    }

    public void addTCListener(TCListener lst) {
        if (lst instanceof NamedTCListener) {
            String name = ((NamedTCListener) lst).getName();
            LOGGER.debug("addTCListener - name={}", name);
            if (ImscfTCAPUtil.isCapStack(name)) {
                if (capListener != null && !capListener.equals(name)) {
                    throw new RuntimeException("TClistener for CAP has been already set");
                }
                this.capListener = (NamedTCListener) lst;
            } else if (ImscfTCAPUtil.isMapStack(name)) {
                if (mapListener != null && !mapListener.equals(name)) {
                    throw new RuntimeException("TClistener for MAP has been already set");
                }
                this.mapListener = (NamedTCListener) lst;
            } else {
                if (!tcListeners.contains(lst)) {
                    tcListeners.add(lst);
                }
            }
        } else {
            if (!tcListeners.contains(lst)) {
                tcListeners.add(lst);
            }
        }
    }

    @Override
    public void onDialogReleased(Dialog d) {
        LOGGER.debug("TCAPListenerImscfAdapter d={} delegating onDialogReleased call", d);
        NamedTCListener listener = findProperListener(d);
        if (listener != null) {
            listener.onDialogReleased(d);
        }
        for (TCListener lst : tcListeners) {
            lst.onDialogReleased(d);
        }
    }

    @Override
    public void onDialogTimeout(Dialog d) {
        LOGGER.debug("TCAPListenerImscfAdapter d={} delegating onDialogTimeout call", d);
        NamedTCListener listener = findProperListener(d);
        if (listener != null) {
            listener.onDialogTimeout(d);
        }
        for (TCListener lst : tcListeners) {
            lst.onDialogTimeout(d);
        }
    }

    @Override
    public void onInvokeTimeout(Invoke tcInvokeRequest) {
        LOGGER.debug("TCAPListenerImscfAdapter tcInvokeRequest={} delegating onInvokeTimeout call", tcInvokeRequest);
        if (tcInvokeRequest instanceof InvokeImpl) {
            Dialog d = ((InvokeImpl) tcInvokeRequest).getDialog();
            NamedTCListener listener = findProperListener(d);
            if (listener != null) {
                listener.onInvokeTimeout(tcInvokeRequest);
            }
            for (TCListener lst : tcListeners) {
                lst.onInvokeTimeout(tcInvokeRequest);
            }
        }
    }

    private void rewriteLocalAddressIfGTPresentAndRouteOnSsn(Dialog d) {
        SccpAddress localAddress = d.getLocalAddress();
        if (localAddress.getGlobalTitle() != null
                && localAddress.getAddressIndicator().getRoutingIndicator() != RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE) {

            SccpAddress newLocalAddress = new SccpAddress(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE,
                    localAddress.getSignalingPointCode(), localAddress.getGlobalTitle(),
                    localAddress.getSubsystemNumber());
            d.setLocalAddress(newLocalAddress);
            LOGGER.debug("rewriteCalledPartyIfGTPresent from={} to={}", localAddress, newLocalAddress);
        }
    }

    @Override
    public void onTCBegin(TCBeginIndication ind) {
        LOGGER.debug("TCAPListenerImscfAdapter ind={} delegating onTCBegin call", ind);
        rewriteLocalAddressIfGTPresentAndRouteOnSsn(ind.getDialog());
        if (OverloadProtector.getInstance().getCurrentState().isCpuOrHeapOverloaded()) {
            LOGGER.debug("System is overloaded. Send back TCAP ABORT.");
            TCUserAbortRequest uabort = tcapProvider.getDialogPrimitiveFactory().createUAbort(ind.getDialog());
            try {
                ind.getDialog().send(uabort);
            } catch (TCAPSendException e) {
                LOGGER.error("Cannot send U-ABORT for overload protection.", e);
            }
            // Do not pass to listeners
            return;
        }
        NamedTCListener listener = findProperListener(ind.getDialog());
        if (listener != null) {
            listener.onTCBegin(ind);
        }
        for (TCListener lst : tcListeners) {
            lst.onTCBegin(ind);
        }
    }

    @Override
    public void onTCContinue(TCContinueIndication ind) {
        LOGGER.debug("TCAPListenerImscfAdapter ind={} delegating onTCContinue call", ind);
        NamedTCListener listener = findProperListener(ind.getDialog());
        if (listener != null) {
            LOGGER.debug("onTCContinue is delegated to listener={}", listener);
            listener.onTCContinue(ind);
        }
        for (TCListener lst : tcListeners) {
            lst.onTCContinue(ind);
        }
    }

    @Override
    public void onTCEnd(TCEndIndication ind) {
        LOGGER.debug("TCAPListenerImscfAdapter ind={} delegating onTCEnd call", ind);
        NamedTCListener listener = findProperListener(ind.getDialog());
        if (listener != null) {
            listener.onTCEnd(ind);
        }
        for (TCListener lst : tcListeners) {
            lst.onTCEnd(ind);
        }
    }

    @Override
    public void onTCNotice(TCNoticeIndication ind) {
        LOGGER.debug("TCAPListenerImscfAdapter ind={} delegating onTCNotice call", ind);
        NamedTCListener listener = findProperListener(ind.getDialog());
        if (listener != null) {
            listener.onTCNotice(ind);
        }
        for (TCListener lst : tcListeners) {
            lst.onTCNotice(ind);
        }
    }

    @Override
    public void onTCPAbort(TCPAbortIndication ind) {
        LOGGER.debug("TCAPListenerImscfAdapter ind={} delegating onTCPAbort call", ind);
        NamedTCListener listener = findProperListener(ind.getDialog());
        if (listener != null) {
            listener.onTCPAbort(ind);
        }
        for (TCListener lst : tcListeners) {
            lst.onTCPAbort(ind);
        }
    }

    @Override
    public void onTCUni(TCUniIndication ind) {
        LOGGER.debug("TCAPListenerImscfAdapter ind={} delegating onTCUni call");
        NamedTCListener listener = findProperListener(ind.getDialog());
        if (listener != null) {
            listener.onTCUni(ind);
        }
        for (TCListener lst : tcListeners) {
            lst.onTCUni(ind);
        }
    }

    @Override
    public void onTCUserAbort(TCUserAbortIndication ind) {
        LOGGER.debug("TCAPListenerImscfAdapter ind={} delegating onTCUserAbort call", ind);
        NamedTCListener listener = findProperListener(ind.getDialog());
        if (listener != null) {
            listener.onTCUserAbort(ind);
        }
        for (TCListener lst : tcListeners) {
            lst.onTCUserAbort(ind);
        }
    }

}
