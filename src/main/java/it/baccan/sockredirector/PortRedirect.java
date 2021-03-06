/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.sockredirector;

import it.baccan.sockredirector.pojo.ServerPojo;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PortRedirect engine.
 *
 * @author Matteo Baccan
 */
public class PortRedirect extends Thread {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PortRedirect.class);

    private final ServerPojo serverPojo;

    /**
     * PortRedirect constructor.
     *
     * @param server
     */
    public PortRedirect(ServerPojo server) {
        super();
        setName("PortRedirect");
        serverPojo = server;
        LOG.info("Ready on [{}:{}] -> [{}:{}] TIMEOUT [{}]", serverPojo.getSourceAddress(), serverPojo.getSourcePort(), serverPojo.getDestinationAddress(), serverPojo.getDestinationPort(), serverPojo.getTimeout());
    }

    @Override
    public final void run() {
        try (ServerSocket sock = new ServerSocket(serverPojo.getSourcePort(), serverPojo.getMaxclient(), InetAddress.getByName(serverPojo.getSourceAddress()))) {
            while (true) {
                // Faccio partire il Thread
                Socket socket = sock.accept();

                // Metto anche il serverPojo.getTimeout() ai socket
                if (serverPojo.getTimeout() > 0) {
                    socket.setSoTimeout(serverPojo.getTimeout() * 1000);
                }

                Thread thread = new ServerSocketThread(socket, serverPojo);
                thread.start();
            }
        } catch (BindException bind) {
            LOG.error("Address [{}:{}] already in use : [{}]", serverPojo.getSourceAddress(), serverPojo.getSourcePort(), bind.getMessage());
        } catch (IOException e) {
            LOG.error("Error in redirector from [{}] \t to [{}:{}]", serverPojo.getSourcePort(), serverPojo.getDestinationAddress(), serverPojo.getDestinationPort());
            LOG.error("Full error", e);
        }
    }

}
