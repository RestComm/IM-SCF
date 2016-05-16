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
package org.restcomm.imscf.sl.mgmt.sctp;


import java.util.List;
import java.util.Map;

import org.mobicents.protocols.api.Association;
import org.mobicents.protocols.api.IpChannelType;
import org.mobicents.protocols.api.Management;
import org.mobicents.protocols.api.ManagementEventListener;
import org.mobicents.protocols.api.Server;
import org.mobicents.protocols.api.ServerListener;

/**
 * Abstract implementation of the Management and Wrapper interface.
 * Implements the delegate pattern but does not implement the method of the Wrapper interface.
 *
 */
public abstract class ImscfManagedSctpManagementWrapper implements Management, Wrapper {
    protected final Management delegatedManagement;

    protected ImscfManagedSctpManagementWrapper(Management delegatedManagement) {
        this.delegatedManagement = delegatedManagement;
    }

    @Override
    public abstract boolean isWrapperFor(Class<?> iface);

    @Override
    public abstract <T> T unwrap(Class<T> iface);

    @Override
    public String getName() {
        return this.delegatedManagement.getName();
    }

    @Override
    public String getPersistDir() {
        return this.delegatedManagement.getPersistDir();
    }

    @Override
    public void setPersistDir(String persistDir) {
        this.delegatedManagement.setPersistDir(persistDir);
    }

    @Override
    public ServerListener getServerListener() {
        return this.delegatedManagement.getServerListener();
    }

    @Override
    public void setServerListener(ServerListener serverListener) {
        this.delegatedManagement.setServerListener(serverListener);
    }

    @Override
    public void addManagementEventListener(ManagementEventListener listener) {
        this.delegatedManagement.addManagementEventListener(listener);
    }

    @Override
    public void removeManagementEventListener(ManagementEventListener listener) {
        this.delegatedManagement.removeManagementEventListener(listener);
    }

    @Override
    public void start() throws Exception {
        this.delegatedManagement.start();
    }

    @Override
    public void stop() throws Exception {
        this.delegatedManagement.stop();
    }

    @Override
    public boolean isStarted() {
        return this.delegatedManagement.isStarted();
    }

    @Override
    public void removeAllResourses() throws Exception {
        this.delegatedManagement.removeAllResourses();
    }

    @Override
    public Server addServer(String serverName, String hostAddress, int port, IpChannelType ipChannelType,
            boolean acceptAnonymousConnections, int maxConcurrentConnectionsCount, String[] extraHostAddresses)
            throws Exception {
        return this.delegatedManagement.addServer(serverName, hostAddress, port, ipChannelType,
                acceptAnonymousConnections, maxConcurrentConnectionsCount, extraHostAddresses);
    }

    @Override
    public Server addServer(String serverName, String hostAddress, int port, IpChannelType ipChannelType,
            String[] extraHostAddresses) throws Exception {
        return this.delegatedManagement.addServer(serverName, hostAddress, port, ipChannelType, extraHostAddresses);
    }

    @Override
    public Server addServer(String serverName, String hostAddress, int port) throws Exception {
        return this.delegatedManagement.addServer(serverName, hostAddress, port);
    }

    @Override
    public void removeServer(String serverName) throws Exception {
        this.delegatedManagement.removeServer(serverName);
    }

    @Override
    public void startServer(String serverName) throws Exception {
        this.delegatedManagement.startServer(serverName);
    }

    @Override
    public void stopServer(String serverName) throws Exception {
        this.delegatedManagement.stopServer(serverName);
    }

    @Override
    public List<Server> getServers() {
        return this.delegatedManagement.getServers();
    }

    @Override
    public Association addServerAssociation(String peerAddress, int peerPort, String serverName, String assocName)
            throws Exception {
        return this.delegatedManagement.addServerAssociation(peerAddress, peerPort, serverName, assocName);
    }

    @Override
    public Association addServerAssociation(String peerAddress, int peerPort, String serverName, String assocName,
            IpChannelType ipChannelType) throws Exception {
        return this.delegatedManagement.addServerAssociation(peerAddress, peerPort, serverName, assocName,
                ipChannelType);
    }

    @Override
    public Association addAssociation(String hostAddress, int hostPort, String peerAddress, int peerPort,
            String assocName) throws Exception {
        return this.delegatedManagement.addAssociation(hostAddress, hostPort, peerAddress, peerPort, assocName);
    }

    @Override
    public Association addAssociation(String hostAddress, int hostPort, String peerAddress, int peerPort,
            String assocName, IpChannelType ipChannelType, String[] extraHostAddresses) throws Exception {
        return this.delegatedManagement.addAssociation(hostAddress, hostPort, peerAddress, peerPort, assocName,
                ipChannelType, extraHostAddresses);
    }

    @Override
    public void removeAssociation(String assocName) throws Exception {
        this.delegatedManagement.removeAssociation(assocName);
    }

    @Override
    public Association getAssociation(String assocName) throws Exception {
        return this.delegatedManagement.getAssociation(assocName);
    }

    @Override
    public Map<String, Association> getAssociations() {
        return this.delegatedManagement.getAssociations();
    }

    @Override
    public void startAssociation(String assocName) throws Exception {
        this.delegatedManagement.startAssociation(assocName);
    }

    @Override
    public void stopAssociation(String assocName) throws Exception {
        this.delegatedManagement.stopAssociation(assocName);
    }

    @Override
    public int getConnectDelay() {
        return this.delegatedManagement.getConnectDelay();
    }

    @Override
    public void setConnectDelay(int connectDelay) throws Exception {
        this.delegatedManagement.setConnectDelay(connectDelay);
    }

    @Override
    public int getWorkerThreads() {
        return this.delegatedManagement.getWorkerThreads();
    }

    @Override
    public void setWorkerThreads(int workerThreads) throws Exception {
        this.delegatedManagement.setWorkerThreads(workerThreads);
    }

    @Override
    public boolean isSingleThread() {
        return this.delegatedManagement.isSingleThread();
    }

    @Override
    public void setSingleThread(boolean singleThread) throws Exception {
        this.delegatedManagement.setSingleThread(singleThread);
    }
}
