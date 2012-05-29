/*
 * @(#)Server.java
 * Created: 2005-04-21
 * Version: 2-0-alpha
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package webag3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import ag3.interfaces.methods.venue.EnterResponse;
import ag3.interfaces.methods.venue.GetStateResponse;
import ag3.interfaces.methods.venue.NegotiateCapabilitiesResponse;
import ag3.interfaces.methods.venue.UpdateLifetimeResponse;
import ag3.interfaces.types.ApplicationDescription;
import ag3.interfaces.types.Capability;
import ag3.interfaces.types.ClientProfile;
import ag3.interfaces.types.ConnectionDescription;
import ag3.interfaces.types.DataDescription;
import ag3.interfaces.types.MulticastNetworkLocation;
import ag3.interfaces.types.NetworkLocation;
import ag3.interfaces.types.ProviderProfile;
import ag3.interfaces.types.ServiceDescription;
import ag3.interfaces.types.StreamDescription;
import ag3.interfaces.types.UnicastNetworkLocation;
import ag3.interfaces.types.VenueState;
import ag3.soap.SoapDeserializer;

/**
 * The main class of the arena RTSP Server. This class runs the server.
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha
 */
public class Server {

    // The name of the file holding the default web values
    private static final String DEFAULT_WEB_DESCRIPTOR = "etc/webdefaults.xml";

    /**
     * The character that separates a host from a port
     */
    public static final String PORT_SEPARATOR = ":";

    // The argument for the xml config file
    private static final String XML_ARGUMENT = "-xml";

    // The argument for the port number of the server
    private static final String PORT_ARGUMENT = "-p";

    /**
     * The home page of the web server
     */
    public static final String WEB_ROOT_FILES = "www/";

    // The RTSP port to listen on
    private int iListenPort = 5400;

    // The Web server
    private org.mortbay.jetty.Server server = null;

    // The shutdown hook
    private DoShutdown ds;

    // The log file
    private static Log logger = LogFactory.getLog(Server.class.getName());

    static {
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/Capability",
                Capability.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/VenueState",
                VenueState.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/ClientProfile",
                ClientProfile.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/ProviderProfile",
                ProviderProfile.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/ApplicationDescription",
                ApplicationDescription.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/ConnectionDescription",
                ConnectionDescription.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/DataDescription",
                DataDescription.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/ServiceDescription",
                ServiceDescription.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/StreamDescription",
                StreamDescription.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/MulticastNetworkLocation",
                MulticastNetworkLocation.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/UnicastNetworkLocation",
                UnicastNetworkLocation.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/NetworkLocation",
                NetworkLocation.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/venue/GetStateResponse",
                GetStateResponse.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/venue/EnterResponse",
                EnterResponse.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/venue/UpdateLifetimeResponse",
                UpdateLifetimeResponse.class);
        SoapDeserializer.mapType(
                "http://www.accessgrid.org/v3.0/venue/"
                + "NegotiateCapabilitiesResponse",
                NegotiateCapabilitiesResponse.class);
    }

    /**
     * Creates a new arenaServer.
     *
     * @param args
     *            an array of command line arguments.
     * @throws Exception
     */
    public Server(String args[]) throws Exception {

        // Search for and extract the arguments
        for (int i = 0; i < args.length; i++) {

            // The port number (-p <PORT>) - optional
            if (args[i].equals(PORT_ARGUMENT)) {
                iListenPort = Integer.valueOf(args[i + 1]).intValue();
                i++;
            }

            // The xml config argument
            else if (args[i].equals(XML_ARGUMENT)) {
                XMLParser parser = new XMLParser();
                BufferedReader reader =
                    new BufferedReader(new FileReader(args[i + 1]));
                i++;
                parser.parse(reader);
            }

            // If the argument is unrecognised, print the usage instructions
            else {
                printUsage();
            }
        }
        
        // Add the shutdown processor
        try {
            ds = new DoShutdown(this);
            Runtime.getRuntime().addShutdownHook(ds);
        } catch (Exception e) {
            // Do Nothing
        }

        initialiseServerObjects();
    }

    // Prints out usage instructions
    private void printUsage() {
        System.err.println("webag3.Server [-p <port>]");
        System.err.println("    -p <port> The port on which to serve the Jabber"
                + " pages (default 5400)");
        System.exit(1);
    }

    // Sets up the server objects
    private void initialiseServerObjects() throws Exception {

        // Set up a web server
        File jettyDir = new File(WEB_ROOT_FILES);
        if (!jettyDir.exists()) {
            jettyDir.mkdirs();
        }
        System.setProperty("jetty.home", WEB_ROOT_FILES);
        server = new org.mortbay.jetty.Server();
        SocketListener listener = new SocketListener();
        listener.setPort(iListenPort);
        listener.setMaxThreads(500);
        server.addListener(listener);

        // Add the context
        WebApplicationHandler servlets = new WebApplicationHandler();
        WebApplicationContext context = new WebApplicationContext();
        context.setDefaultsDescriptor(DEFAULT_WEB_DESCRIPTOR);
        context.setContextPath("/");
        context.addHandler(servlets);
        context.addHandler(new ResourceHandler());
        context.addHandler(new NotFoundHandler());
        context.setWAR("www/");
        server.addContext(context);

        // Start the server
        server.start();
		logger.info("Server Started.");
    }

    /**
     * Stops the server
     */
    public void stop() {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (InterruptedException e) {
            // Do Nothing
        }
        logger.info("Server Shutting Down");
        new DoExit().start();
    }

    // Exits the application cleanly
    private class DoExit extends Thread {

        /**
         *
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Do Nothing
            }
            Runtime.getRuntime().removeShutdownHook(ds);
            logger.info("Server Stopped.");
//            System.err.println("Server Stopped.");
//            System.exit(0);
        }
    }

    // Used to detect when the server should be stopped
    private class DoShutdown extends Thread {

        // The server
        Server serv = null;

        /**
         * Creates a shutdown handler
         * @param server The server to shutdown
         */
        public DoShutdown(Server server) {
            this.serv = server;
        }

        /**
         *
         * @see java.lang.Runnable#run()
         */
        public void run() {
            // Stop the server
            serv.stop();
        }
    }

    /**
     * The Main method
     *
     * @param args
     *            The arguments
     */
    public static void main(String args[]) {
        try {
            System.setProperty("java.net.preferIPv4Stack","true");
            new Server(args);
        } catch (Exception e) {
            logger.error("Exception ", e);
            System.exit(-1);
        }
    }

    private class XMLParser extends DefaultHandler {

        // The xml attribute for the port
        private static final String PORT_VALUE_XML_ARGUMENT = "value";

        // The xml tag for the port
        private static final String PORT_XML_ARGUMENT = "port";

        // The xml tag for the configuration
        private static final String CONFIGURATION_XML_ARGUMENT = "config";

        // The xml parser to use
        private static final String XML_PARSER_IMPLEMENTATION =
            "org.apache.xerces.parsers.SAXParser";

        // The xml parser
        private XMLReader parser = null;

        // True if the xml is currently in the config tag
        private boolean inConfig = false;

        private XMLParser() throws SAXException {
            parser =
                XMLReaderFactory.createXMLReader(
                        XML_PARSER_IMPLEMENTATION);
            parser.setContentHandler(this);
        }

        private void parse(Reader reader) throws IOException, SAXException {
            InputSource source = new InputSource(reader);
            parser.parse(source);
        }

        /**
         *
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         *     java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(String namespaceURI, String localName,
                String qualifiedName, Attributes atts) throws SAXException {
            if (localName.equals(CONFIGURATION_XML_ARGUMENT)) {
                if (!inConfig) {
                    inConfig = true;
                } else {
                    throw new SAXException("Error parsing XML in Config: "
                            + localName);
                }
            } else if (!inConfig) {
                throw new SAXException("Error parsing XML at top level: "
                        + localName);
            } else if (localName.equals(PORT_XML_ARGUMENT)) {
                iListenPort =
                    Integer.parseInt(atts.getValue(PORT_VALUE_XML_ARGUMENT));
            } else {
                throw new SAXException("Error parsing XML: unknown element "
                        + localName);
            }
        }
    }
}
