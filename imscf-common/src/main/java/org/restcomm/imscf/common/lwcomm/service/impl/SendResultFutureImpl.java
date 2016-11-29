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
package org.restcomm.imscf.common.lwcomm.service.impl;

import org.restcomm.imscf.common.lwcomm.service.FutureListener;
import org.restcomm.imscf.common.lwcomm.service.SendResult;
import org.restcomm.imscf.common.lwcomm.service.SendResultFuture;
import org.restcomm.imscf.common.lwcomm.service.TextMessage;
import org.restcomm.imscf.common.lwcomm.service.messages.MessageSender;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Return type of the send operation in LwCommService.
 * This is a standard java.util.concurrent.Future implementation with a listenable support.
 * @author Miklos Pocsaji
 *
 */
public class SendResultFutureImpl implements SendResultFuture<SendResult> {

    private MessageSender messageSender;
    private boolean cancelled;
    private boolean done;
    private SendResult result;
    private String messageId;
    private TextMessage originalMessage;
    private LinkedHashMap<FutureListener<SendResult>, Executor> listeners = new LinkedHashMap<FutureListener<SendResult>, Executor>();

    public SendResultFutureImpl(TextMessage originalMessage) {
        this.originalMessage = originalMessage;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this) {
            cancelled = true;
            done = true;
            result = SendResult.CANCELLED;
            notifyAll();
        }
        return messageSender.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized SendResult get() throws InterruptedException, ExecutionException {
        if (cancelled || done) {
            return result;
        } else {
            while (!cancelled && !done)
                wait();
            return result;
        }
    }

    @Override
    public synchronized SendResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        if (cancelled || done) {
            return result;
        } else {
            unit.timedWait(this, timeout);
            return result;
        }
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public synchronized void done(SendResult result) {
        this.result = result;
        done = true;
        notifyAll();
        for (Map.Entry<FutureListener<SendResult>, Executor> entry : listeners.entrySet()) {
            Executor e = entry.getValue();
            callListener(entry.getKey(), e);
        }
    }

    private void callListener(FutureListener<SendResult> listener, Executor e) {
        if (e == null) {
            listener.done(this);
        } else {
            e.execute(new ListenerCaller(listener));
        }
    }

    @Override
    public synchronized void addListener(FutureListener<SendResult> listener, Executor executor) {
        if (cancelled || done) {
            callListener(listener, executor);
        } else {
            listeners.put(listener, executor);
        }
    }

    @Override
    public synchronized void removeListener(FutureListener<SendResult> listener) {
        listeners.remove(listener);
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public TextMessage getOriginalMessage() {
        return originalMessage;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Runnable which calls the future listener.
     * Used when the future listener is added by specifying the executor.
     * @author Miklos Pocsaji
     *
     */
    private class ListenerCaller implements Runnable {
        private FutureListener<SendResult> listenerToCall;

        public ListenerCaller(FutureListener<SendResult> listener) {
            this.listenerToCall = listener;
        }

        @Override
        public void run() {
            listenerToCall.done(SendResultFutureImpl.this);
        }

    }
}
