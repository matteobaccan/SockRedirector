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

import it.baccan.sockredirector.pojo.ServerPojo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matteo Baccan <matteo@baccan.it>
 */
public class SockRedirector extends Thread {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SockRedirector.class);

    /**
     *
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

        LOG.info("+---------------------------------------------------------------------------+");
        LOG.info("| TCP/IP Port Redirector                                                    |");
        LOG.info("| Matteo Baccan Opensource Software                    http://www.baccan.it |");
        LOG.info("+---------------------------------------------------------------------------+");
        LOG.info("");

        LOG.info("Setup environment");
        // Se non ho la directory di LOG la creo
        try {
            File oFile = new File("logs");
            if (!oFile.exists()) {
                oFile.mkdir();
            }
        } catch (Throwable e) {
            LOG.error("Error creating log directory", e);
        }

        LOG.info("Opening [{}] ...", initFile);
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
                String cXML = cFileReaded.substring(nPosIni, nPosEnd + cEnd.length());

                ServerPojo serverPojo = new ServerPojo();
                serverPojo.setSourceAddress(getToken(cXML, "source"));
                serverPojo.setSourcePort(Integer.parseInt(getToken(cXML, "sourceport")));
                serverPojo.setDestinationAddress(getToken(cXML, "destination"));
                serverPojo.setDestinationPort(Integer.parseInt(getToken(cXML, "destinationport")));
                serverPojo.setLogger(getToken(cXML, "log", "false").equalsIgnoreCase("true"));

                // Se uno notes deve essere messo a 0
                serverPojo.setTimeout(Integer.parseInt(getToken(cXML, "timeout", "0")));
                serverPojo.setMaxclient(Integer.parseInt(getToken(cXML, "client", "10")));

                serverPojo.setBlockSize(Integer.parseInt(getToken(cXML, "blocksize", "64000")));

                PortRedirect server = new PortRedirect(serverPojo);
                server.start();

                // Cerco il blocco successivo
                cFileReaded = cFileReaded.substring(nPosEnd + cEnd.length());
                nPosIni = cFileReaded.indexOf(cIni);
                nPosEnd = cFileReaded.indexOf(cEnd);
            }
            // Start admin
            admin.start();

            LOG.info("");
            LOG.info("All system ready. Type \"help\" for debug info");

        } catch (IOException e) {
            LOG.error("Error during opening of [{}]", initFile);
            LOG.error("IOException", e);
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
