/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.sockredirector.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Matteo
 */
@Setter @Getter
public class ServerPojo {

    private String sourceAddress;
    private int sourcePort;
    private String destinationAddress;
    private int destinationPort;
    private boolean logger;
    private int timeout;
    private int maxclient;
    private int blockSize;
}
