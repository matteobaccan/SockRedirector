/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 *
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
package it.baccan.sockredirector.pojo;

import lombok.Data;

/** @author Matteo Baccan */
@Data
public class ServerPojo {

    private String sourceAddress;
    private int sourcePort;
    private String destinationAddress;
    private int destinationPort;
    private boolean logger;
    private int timeout;
    private int maxclient;
    private int blockSize;
    private long inReadWait;
    private long inWriteWait;
    private long outReadWait;
    private long outWriteWait;
}
