/*
 * Copyright (c) 2019 Matteo Baccan
 * https://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or https://www.gnu.org/licenses/gpl-3.0.html.
 *
 */
package it.baccan.sockredirector;

import it.baccan.sockredirector.pojo.ServerPojo;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

/**
 * PortRedirect engine.
 *
 * @author Matteo Baccan
 */
@Slf4j
public class PortRedirect extends Thread {

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
        log.info(
                "Ready on [{}:{}] -> [{}:{}] TIMEOUT [{}]",
                serverPojo.getSourceAddress(),
                serverPojo.getSourcePort(),
                serverPojo.getDestinationAddress(),
                serverPojo.getDestinationPort(),
                serverPojo.getTimeout());
    }

    /**
     * Execute port redirect thread.
     */
    @Override
    public final void run() {
        try (ServerSocket sock
                = new ServerSocket(
                        serverPojo.getSourcePort(),
                        serverPojo.getMaxclient(),
                        InetAddress.getByName(serverPojo.getSourceAddress()))) {
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
            log.error(
                    "Address [{}:{}] already in use : [{}]",
                    serverPojo.getSourceAddress(),
                    serverPojo.getSourcePort(),
                    bind.getMessage());
        } catch (IOException e) {
            log.error(
                    "Error in redirector from [{}] \t to [{}:{}]",
                    serverPojo.getSourcePort(),
                    serverPojo.getDestinationAddress(),
                    serverPojo.getDestinationPort());
            log.error("Full error", e);
        }
    }
}
