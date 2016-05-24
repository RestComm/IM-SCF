/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011­2016, Telestax Inc and individual contributors
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
package org.restcomm.imscf.sl.diameter.impl;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.DisconnectCause;
import org.jdiameter.api.ResultCode;
import org.jdiameter.api.app.State;
import org.jdiameter.api.app.StateEvent;
import org.jdiameter.client.api.IMessage;
import org.jdiameter.client.api.fsm.IContext;
import org.jdiameter.client.impl.fsm.FsmState;
import org.jdiameter.common.api.concurrent.IConcurrentFactory;
import org.jdiameter.common.api.statistic.IStatisticManager;
import org.jdiameter.server.impl.fsm.PeerFSMImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modified final state machine implementation of server side peers.
 *
 * Modifications:
 *      In STOPPING state when DPA event received the server disconnects the transport layer connection and switch to DOWN state
 */
@SuppressWarnings("PMD.GodClass")
public class ImscfServerPeerFSMImpl extends PeerFSMImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImscfServerPeerFSMImpl.class);

    public ImscfServerPeerFSMImpl(IContext context, IConcurrentFactory concurrentFactory, Configuration config,
            IStatisticManager statisticFactory) {
        super(context, concurrentFactory, config, statisticFactory);
    }

    protected State createOKEYState() {
        return new MyState() // OKEY
        {
            public void entryAction() { // todo send buffered messages
                setInActiveTimer();
                watchdogSent = false;
            }

            public boolean processEvent(StateEvent event) {
                switch (type(event)) {
                case DISCONNECT_EVENT:
                    doEndConnection();
                    break;
                case TIMEOUT_EVENT:
                    try {
                        context.sendDwrMessage();
                        setTimer(DWA_TIMEOUT);
                        if (watchdogSent) {
                            switchToNextState(FsmState.SUSPECT);
                        } else {
                            watchdogSent = true;
                        }
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send DWR", e);
                        doDisconnect();
                        doEndConnection();
                    }
                    break;
                case STOP_EVENT:
                    try {
                        if (event.getData() == null) {
                            context.sendDprMessage(DisconnectCause.BUSY);
                        } else {
                            Integer disconnectCause = (Integer) event.getData();
                            context.sendDprMessage(disconnectCause);
                        }
                        setTimer(DPA_TIMEOUT);
                        switchToNextState(FsmState.STOPPING);
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send DPR", e);
                        doDisconnect();
                        switchToNextState(FsmState.DOWN);
                    }
                    break;
                case RECEIVE_MSG_EVENT:
                    setInActiveTimer();
                    context.receiveMessage(message(event));
                    break;
                case CEA_EVENT:
                    setInActiveTimer();
                    if (context.processCeaMessage(key(event), message(event))) {
                        doDisconnect(); // !
                        doEndConnection();
                    }
                    break;
                case CER_EVENT:
                    // setInActiveTimer();
                    LOGGER.debug("Rejecting CER in OKAY state. Answering with UNABLE_TO_COMPLY (5012)");
                    try {
                        context.sendCeaMessage(ResultCode.UNABLE_TO_COMPLY, message(event),
                                "Unable to receive CER in OPEN state.");
                    } catch (Exception e) {
                        LOGGER.debug("Failed to send CEA.", e);
                        doDisconnect(); // !
                        doEndConnection();
                    }
                    break;
                case DPR_EVENT:
                    try {
                        int code = context.processDprMessage((IMessage) event.getData());
                        context.sendDpaMessage(message(event), code, null);
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send DPA", e);
                    }
                    IMessage message = (IMessage) event.getData();
                    try {
                        Avp discCause = message.getAvps().getAvp(Avp.DISCONNECT_CAUSE);
                        boolean willReconnect = (discCause != null) ? (discCause.getInteger32() == DisconnectCause.REBOOTING)
                                : false;
                        if (willReconnect) {
                            doDisconnect();
                            doEndConnection();
                        } else {
                            doDisconnect();
                            switchToNextState(FsmState.DOWN);
                        }
                    } catch (AvpDataException ade) {
                        LOGGER.warn("Disconnect cause is bad.", ade);
                        doDisconnect();
                        switchToNextState(FsmState.DOWN);
                    }

                    break;
                case DWR_EVENT:
                    setInActiveTimer();
                    try {
                        context.sendDwaMessage(message(event), ResultCode.SUCCESS, null);
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send DWA, reconnecting", e);
                        doDisconnect();
                        doEndConnection();
                    }
                    break;
                case DWA_EVENT:
                    setInActiveTimer();
                    watchdogSent = false;
                    break;
                case SEND_MSG_EVENT:
                    try {
                        context.sendMessage(message(event));
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send message", e);
                        doDisconnect();
                        doEndConnection();
                    }
                    break;
                default:
                    LOGGER.debug("Unknown event type {} in state {}", type(event), state);
                    return false;
                }
                return true;
            }
        };
    }

    protected State createSUSPECTState() {
        return new MyState() // SUSPECT
        {
            public boolean processEvent(StateEvent event) {
                switch (type(event)) {
                case DISCONNECT_EVENT:
                    doEndConnection();
                    break;
                case TIMEOUT_EVENT:
                    doDisconnect();
                    doEndConnection();
                    break;
                case STOP_EVENT:
                    try {
                        if (event.getData() == null) {
                            context.sendDprMessage(DisconnectCause.REBOOTING);
                        } else {
                            Integer disconnectCause = (Integer) event.getData();
                            context.sendDprMessage(disconnectCause);
                        }
                        setInActiveTimer();
                        switchToNextState(FsmState.STOPPING);
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send DPR", e);
                        doDisconnect();
                        switchToNextState(FsmState.DOWN);
                    }
                    break;
                case CER_EVENT:
                case CEA_EVENT:
                case DWA_EVENT:
                    clearTimer();
                    switchToNextState(FsmState.OKAY);
                    break;
                case DPR_EVENT:
                    try {
                        int code = context.processDprMessage((IMessage) event.getData());
                        context.sendDpaMessage(message(event), code, null);
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send DPA", e);
                    }
                    IMessage message = (IMessage) event.getData();
                    try {
                        if (message.getAvps().getAvp(Avp.DISCONNECT_CAUSE) != null
                                && message.getAvps().getAvp(Avp.DISCONNECT_CAUSE).getInteger32() == DisconnectCause.REBOOTING) {
                            doDisconnect();
                            doEndConnection();
                        } else {
                            doDisconnect();
                            switchToNextState(FsmState.DOWN);
                        }
                    } catch (AvpDataException e1) {
                        LOGGER.warn("Disconnect cause is bad.", e1);
                        doDisconnect();
                        switchToNextState(FsmState.DOWN);
                    }
                    break;
                case DWR_EVENT:
                    try {
                        int code = context.processDwrMessage((IMessage) event.getData());
                        context.sendDwaMessage(message(event), code, null);
                        switchToNextState(FsmState.OKAY);
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send DWA", e);
                        doDisconnect();
                        switchToNextState(FsmState.DOWN);
                    }
                    break;
                case RECEIVE_MSG_EVENT:
                    clearTimer();
                    context.receiveMessage(message(event));
                    switchToNextState(FsmState.OKAY);
                    break;
                case SEND_MSG_EVENT: // todo buffering
                    throw new IllegalStateException("Connection is down");
                default:
                    LOGGER.debug("Unknown event type {} in state {}", type(event), state);
                    return false;
                }
                return true;
            }
        };
    }

    protected State createDOWNState() {
        return new MyState() // DOWN
        {
            public void entryAction() {
                setTimer(0);
                // FIXME: baranowb: removed this, cause this breaks peers as
                // it seems, if peer is not removed, it will linger
                // without any way to process messages
                // if(context.isRestoreConnection()) {
                // PCB added FSM multithread
                mustRun = false;
                // }
                context.removeStatistics();
            }

            public boolean processEvent(StateEvent event) {
                switch (type(event)) {
                case START_EVENT:
                    try {
                        context.createStatistics();
                        if (!context.isConnected()) {
                            context.connect();
                        }
                        context.sendCerMessage();
                        setTimer(CEA_TIMEOUT);
                        switchToNextState(FsmState.INITIAL);
                    } catch (Throwable e) {
                        LOGGER.debug("Connect error", e);
                        doEndConnection();
                    }
                    break;
                case CER_EVENT:
                    context.createStatistics();
                    int resultCode = context.processCerMessage(key(event), message(event));
                    if (resultCode == ResultCode.SUCCESS) {
                        try {
                            context.sendCeaMessage(resultCode, message(event), null);
                            switchToNextState(FsmState.OKAY);
                        } catch (Exception e) {
                            LOGGER.debug("Failed to send CEA.", e);
                            doDisconnect(); // !
                            doEndConnection();
                        }
                    } else {
                        try {
                            context.sendCeaMessage(resultCode, message(event), null);
                        } catch (Exception e) {
                            LOGGER.debug("Failed to send CEA.", e);
                        }
                        doDisconnect(); // !
                        doEndConnection();
                    }
                    break;
                case SEND_MSG_EVENT:
                    // todo buffering
                    throw new IllegalStateException("Connection is down");
                case STOP_EVENT:
                case TIMEOUT_EVENT:
                case DISCONNECT_EVENT:
                    // those are ~legal, ie. DISCONNECT_EVENT is sent back from connection
                    break;
                default:
                    LOGGER.debug("Unknown event type {} in state {}", type(event), state);
                    return false;
                }
                return true;
            }
        };
    }

    protected State createREOPENState() {
        return new MyState() // REOPEN
        {
            public boolean processEvent(StateEvent event) {
                switch (type(event)) {
                case CONNECT_EVENT:
                    try {
                        context.sendCerMessage();
                        setTimer(CEA_TIMEOUT);
                        switchToNextState(FsmState.INITIAL);
                    } catch (Throwable e) {
                        LOGGER.debug("Can not send CER", e);
                        setTimer(REC_TIMEOUT);
                    }
                    break;
                case TIMEOUT_EVENT:
                    try {
                        context.connect();
                    } catch (Exception e) {
                        LOGGER.debug("Can not connect to remote peer", e);
                        setTimer(REC_TIMEOUT);
                    }
                    break;
                case STOP_EVENT:
                    setTimer(0);
                    doDisconnect();
                    switchToNextState(FsmState.DOWN);
                    break;
                case DISCONNECT_EVENT:
                    break;
                case SEND_MSG_EVENT:
                    // todo buffering
                    throw new IllegalStateException("Connection is down");
                default:
                    LOGGER.debug("Unknown event type {} in state {}", type(event), state);
                    return false;
                }
                return true;
            }
        };
    }

    protected State createINITIALState() {
        return new MyState() // INITIAL
        {
            public void entryAction() {
                setTimer(CEA_TIMEOUT);
            }

            public boolean processEvent(StateEvent event) {
                switch (type(event)) {
                case DISCONNECT_EVENT:
                    setTimer(0);
                    doEndConnection();
                    break;
                case TIMEOUT_EVENT:
                    doDisconnect();
                    doEndConnection();
                    break;
                case STOP_EVENT:
                    setTimer(0);
                    doDisconnect();
                    switchToNextState(FsmState.DOWN);
                    break;
                case CEA_EVENT:
                    setTimer(0);
                    if (context.processCeaMessage(key(event), message(event))) {
                        switchToNextState(FsmState.OKAY);
                    } else {
                        doDisconnect(); // !
                        doEndConnection();
                    }
                    break;
                case CER_EVENT:
                    int resultCode = context.processCerMessage(key(event), message(event));
                    if (resultCode ==  ResultCode.SUCCESS) {
                        try {
                            context.sendCeaMessage(resultCode, message(event), null);
                            switchToNextState(FsmState.OKAY); // if other connection is win
                        } catch (Exception e) {
                            LOGGER.debug("Can not send CEA", e);
                            doDisconnect();
                            doEndConnection();
                        }
                    } else if (resultCode == -1 || resultCode == ResultCode.NO_COMMON_APPLICATION) {
                        doDisconnect();
                        doEndConnection();
                    }
                    break;
                case SEND_MSG_EVENT:
                    // todo buffering
                    throw new IllegalStateException("Connection is down");
                default:
                    LOGGER.debug("Unknown event type {} in state {}", type(event), state);
                    return false;
                }
                return true;
            }
        };
    }

    protected State createSTOPPINGState() {
        return new MyState() // STOPPING
        {
            public boolean processEvent(StateEvent event) {
                switch (type(event)) {
                case TIMEOUT_EVENT:
                case DPA_EVENT:
                    setTimer(0);
                    doDisconnect();
                    LOGGER.debug("BUG_TRACE 1: modified state action executed!");
                    switchToNextState(FsmState.DOWN);
                    break;
                case RECEIVE_MSG_EVENT:
                    context.receiveMessage(message(event));
                    break;
                case SEND_MSG_EVENT:
                    throw new IllegalStateException("Stack now is stopping");
                case STOP_EVENT:
                case DISCONNECT_EVENT:
                    break;
                default:
                    LOGGER.debug("Unknown event type {} in state {}", type(event), state);
                    return false;
                }
                return true;
            }
        };
    }

    protected State[] getStates() {
        if (states == null) {
            states = new State[] { createOKEYState(), createSUSPECTState(), createDOWNState(), createREOPENState(),
                    createINITIALState(), createSTOPPINGState() };
        }
        return states;
    }
}
