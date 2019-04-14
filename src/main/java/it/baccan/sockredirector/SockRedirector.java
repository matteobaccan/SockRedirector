/*
 * Copyright (c) 2019 Matteo Baccan
 * http://www.baccan.it
 * 
 * Distributed under the GPL v3 software license, see the accompanying
 * file LICENSE or http://www.gnu.org/licenses/gpl.html.
 *
 */
/*
 * socketRedirector server
 */
package it.baccan.sockredirector;

import java.io.FileInputStream;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Matteo Baccan <matteo@baccan.it>
 */
@Slf4j
public class SockRedirector extends Thread {

    /**
     *
     * @param argv
     */
    static public void main(String[] argv) {

        final String initFile = "sockRedirector.ini";

        log.info("+---------------------------------------------------------------------------+");
        log.info("| TCP/IP Port Redirector                                                    |");
        log.info("| Matteo Baccan Opensource Software                    http://www.baccan.it |");
        log.info("+---------------------------------------------------------------------------+");
        log.info("");
        log.info("Opening {} ...", initFile);
        try {

            byte[] buffer;
            // Leggo il file di specifiche
            try (InputStream fInput = new FileInputStream(initFile)) {
                buffer = new byte[fInput.available()];
                fInput.read(buffer);
            }
            String cFileReaded = new String(buffer);

            // Essendo in formato XML mi limito a prendere i vari blocchi di
            // redirezione
            String cIni = "<redirection>";
            String cEnd = "</redirection>";
            int nPosIni = cFileReaded.indexOf(cIni);
            int nPosEnd = cFileReaded.indexOf(cEnd);

            // Create admin process
            AdminThread admin = new AdminThread();
            while (nPosEnd > nPosIni) {
                String cRed = cFileReaded.substring(nPosIni, nPosEnd + cEnd.length());

                PortRedirect server = new PortRedirect(cRed);
                server.start();

                // Cerco il blocco successivo
                cFileReaded = cFileReaded.substring(nPosEnd + cEnd.length());
                nPosIni = cFileReaded.indexOf(cIni);
                nPosEnd = cFileReaded.indexOf(cEnd);
            }
            // Start admin
            admin.start();

            log.info("");
            log.info("All system ready. Type \"help\" for debug info");

        } catch (Exception e) {
            log.error("Error during opening of " + initFile, e);
        }
    }
}
