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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Sock redirector server.
 *
 * @author Matteo Baccan
 */
@Slf4j
public class SockRedirector extends Thread {

    /**
     * @param argv Command line parameters.
     */
    public static void main(final String[] argv) {
        SockRedirector sockRedirector = new SockRedirector();
        sockRedirector.init();
    }

    /**
     * Init program.
     */
    public void init() {
        final String initFile = "sockRedirector.ini";

        System.setProperty("sun.net.spi.nameservice.nameservers", "8.8.8.8");
        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");

        log.info("+---------------------------------------------------------------------------+");
        log.info("| TCP/IP Port Redirector                                                    |");
        log.info("| Matteo Baccan Opensource Software                   https://www.baccan.it |");
        log.info("+---------------------------------------------------------------------------+");
        log.info("");

        log.info("Setup environment");
        // Se non ho la directory di LOG la creo
        try {
            File oFile = new File("logs");
            if (!oFile.exists()) {
                oFile.mkdir();
            }
        } catch (Throwable e) {
            log.error("Error creating log directory", e);
        }

        log.info("Opening [{}] ...", initFile);
        try {
            byte[] buffer;
            File config = new File(initFile);
            if (config.exists()) {
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
                    String cXML = cFileReaded.substring(nPosIni, nPosEnd + cEnd.length());

                    // Service definition
                    ServerPojo serverPojo = new ServerPojo();
                    serverPojo.setSourceAddress(getToken(cXML, "source"));
                    serverPojo.setSourcePort(Integer.parseInt(getToken(cXML, "sourceport")));
                    serverPojo.setDestinationAddress(getToken(cXML, "destination"));
                    serverPojo.setDestinationPort(
                            Integer.parseInt(getToken(cXML, "destinationport")));
                    serverPojo.setLogger(getToken(cXML, "log", "false").equalsIgnoreCase("true"));
                    serverPojo.setTimeout(Integer.parseInt(getToken(cXML, "timeout", "0")));
                    serverPojo.setMaxclient(Integer.parseInt(getToken(cXML, "client", "10")));
                    serverPojo.setBlockSize(Integer.parseInt(getToken(cXML, "blocksize", "64000")));
                    serverPojo.setInReadWait(Long.parseLong(getToken(cXML, "inReadWait", "0")));
                    serverPojo.setInWriteWait(Long.parseLong(getToken(cXML, "inWriteWait", "0")));
                    serverPojo.setOutReadWait(Long.parseLong(getToken(cXML, "outReadWait", "0")));
                    serverPojo.setOutWriteWait(Long.parseLong(getToken(cXML, "outWriteWait", "0")));
                    serverPojo.setRandomKill(Long.parseLong(getToken(cXML, "randomKill", "0")));

                    // Start server
                    PortRedirect server = new PortRedirect(serverPojo);
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
            } else {
                log.error("Missing configuration file [{}]", config.getAbsolutePath());
            }

        } catch (IOException e) {
            log.error("Error during opening of [{}]", initFile);
            log.error("IOException", e);
        }
    }

    private String getToken(String cXML, String cToken) {
        return getToken(cXML, cToken, "");
    }

    private String getToken(String cXML, String cToken, String cDefault) {
        String cRet = "";

        String cIni = "<" + cToken + ">";
        String cEnd = "</" + cToken + ">";
        int nPosIni = cXML.indexOf(cIni);
        int nPosEnd = cXML.indexOf(cEnd);

        if (nPosEnd > nPosIni) {
            cRet = cXML.substring(nPosIni + cIni.length(), nPosEnd);
        }

        if (cRet.length() == 0) {
            cRet = cDefault;
        }

        return cRet;
    }
}
