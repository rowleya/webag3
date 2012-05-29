/*
 * @(#)Client.java
 * Created: 31-May-2006
 * Version: 1
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.GroupChat;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SSLXMPPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.packet.DelayInformation;

/**
 * Represents a client connection to a jabber room
 * @author Andrew G D Rowley
 * @version 1
 */
public class JabberClient implements PacketListener {

    private static final String[] COLORS = new String[]{"#FF0000", "#00FF00",
        "#0000FF", "#FF00FF", "#00FFFF"};

    private static final String ROOM_COLOR = "#00CC00";

    private int currentColorIndex = COLORS.length - 1;

    private XMPPConnection connection = null;

    private GroupChat groupChat = null;

    private LinkedList<String> queue = new LinkedList<String>();

    private boolean done = false;

    private long lastAccess = 0;

    private String groupName = null;

    private HashMap<String, String> userColor = new HashMap<String, String>();

    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private boolean clientWaiting = false;

    private Vector<String> roster = new Vector<String>();

    private boolean rosterChanged = false;

    private int waitTime = 10000;

    private boolean ignorePastMessages = false;

    private long lastTimeout = 0; 
    
    private int timeout = 15 * 60 * 1000; // milisecs.
    
    /**
     * How long will we wait for a messagge before telling
       the browser there is none. This is mainly to stop in IE
       the request itself from timing out.
     */
    private boolean checkTimeout() {
		long time = System.currentTimeMillis();
		if((time - lastTimeout) > timeout) {
			lastTimeout = time;
			return true;
		}
		return false;
	}
    
    // Gets all the messages in the queue
    private String getMessages() {
        String messages = "Done";
        synchronized (queue) {
            while (!done && queue.isEmpty() && !checkTimeout()) {
                try {
                    clientWaiting = true;
                    queue.wait(waitTime);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            clientWaiting = false;
            if (!queue.isEmpty()) {
                String message = (String) queue.removeFirst();
                messages = message;
            } else if (done == false) {
                messages = "None"; // got here because of Timeout()
            }
        }
        return messages;
    }

    // Returns the color for the user
    private String getColor(String from) {
        String color = userColor.get(from);
        if (color == null) {
            currentColorIndex = (currentColorIndex + 1) % COLORS.length;
            color = COLORS[currentColorIndex];
            userColor.put(from, color);
        }
        return color;
    }

    // Adds a message to the queue
    private void addMessage(Date date, String from, String message) {
        synchronized (queue) {
            String data = "";
            if (date != null) {
                data += "[" + dateFormat.format(date) + "] ";
            }
            String color = ROOM_COLOR;
            if (from != null) {
                from = from.replace(groupName, "");
                if (from.startsWith("/")) {
                    from = from.substring(1);
                }
                color = getColor(from);
                if (!from.equals("")) {
                    data += "&lt;" + from + "&gt; ";
                } else {
                    data += "*** ";
                }
            }
            if (message != null) {
                Pattern url = Pattern.compile("(^|[ \t\r\n])((ftp|http|https|" +
                        "gopher|mailto|news|nntp|telnet|wais|file|prospero|" +
                        "aim|webcal):(([A-Za-z0-9$_.+!*(),;/?:@&~=-])|%" +
                        "[A-Fa-f0-9]{2}){2,}(#([a-zA-Z0-9][a-zA-Z0-9$_.+!" +
                        "*(),;/?:@&~=%-]*))?([A-Za-z0-9$_+!*();/?:~-]))");
                Matcher matcher = url.matcher(message);
                String newMessage = "";
                int lastEnd = 0;
                while (matcher.find()) {
                    int start = matcher.start();
                    if (start != 0) {
                        start += 1;
                    }
                    int end = matcher.end();
                    newMessage += message.substring(lastEnd, start);
                    lastEnd = end;
                    newMessage += "<a href='" + message.substring(start, end)
                                   + "' target='_blank'>";
                    newMessage += message.substring(start, end);
                    newMessage += "</a>";
                }
                newMessage += message.substring(lastEnd);
                data += newMessage;
            }
            //data = data.replaceAll("'", "\\\\'");
            data = data.replaceAll("\n", "<br>");
            queue.addLast("Message:" +
                    "<span style='color: " + color + ";'>" + data +
                    "</span><br/>");
            queue.notify();
        }
    }

    private void addRoster(String name) {
        synchronized (queue) {
            queue.addLast("RosterAdd:" + name);
            queue.notify();
        }
    }

    private void removeRoster(String name) {
        synchronized (queue) {
            queue.addLast("RosterRemove:" + name);
            queue.notify();
        }
    }

    /**
     * Creates a new Client
     * @param server The server to connect to
     * @param port The port to connect to
     * @param secure True if the connection is ssl
     * @param roomname The name of the room to connect to
     * @param nickname The name to display in the room
     * @throws XMPPException
     */
    public JabberClient(String server, int port, boolean secure, String roomname,
            String nickname)
            throws XMPPException {
        this.groupName = roomname;
        userColor.put("", ROOM_COLOR);
        if (!secure) {
            connection = new XMPPConnection(server, port);
        } else {
            connection = new SSLXMPPConnection(server, port);
        }
        String username = "user" + System.currentTimeMillis();
        String password = String.valueOf(System.currentTimeMillis());
        AccountManager accounts = connection.getAccountManager();
        accounts.createAccount(username, password);
        connection.login(username, password);
        groupChat = connection.createGroupChat(roomname);
        groupChat.addMessageListener(this);
        groupChat.addParticipantListener(this);
        groupChat.join(nickname.replace("'", "&apos;"));

        lastAccess = System.currentTimeMillis();

        Thread keepalive = new Thread() {
            public void run() {
                Presence presence = new Presence(Presence.Type.AVAILABLE);
                while (!done) {
                    connection.sendPacket(presence);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // Do Nothing
                    }
                }
            }
        };
        keepalive.start();

        if (groupChat != null) {
            Iterator<?> iter = groupChat.getParticipants();
            while (iter.hasNext()) {
                String from = (String) iter.next();
                from = from.replace(groupName, "");
                if (from.startsWith("/")) {
                    from = from.substring(1);
                }
                if (!from.equals("") && !roster.contains(from)) {
                    roster.add(from);
                    addRoster(from);
                }
            }
        }

        ConnectionListener connection_listener = new ConnectionListener() {
            public void connectionClosed(){
                // Don't do anything for expected connection closures
            }
            public void connectionClosedOnError(Exception e){
                e.printStackTrace();
                addMessage(new Date(), "", "Error: " + e.getMessage());
                close();
            }
        };
        connection.addConnectionListener(connection_listener);

        Thread cull = new Thread() {
            public void run() {
                while (!done) {
                    if ((System.currentTimeMillis() - lastAccess) >
                            (waitTime * 2)) {
                        if (!clientWaiting) {
                            System.err.println("Client timed out");
                            addMessage(new Date(), "",
                                    "Client appears to have disconnected ***");
                            close();
                        }
                    }
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        // Do Nothing
                    }
                }
            }
        };
        cull.start();

        System.err.println("Created Client");
    }

    /**
     *
     * @see org.jivesoftware.smack.PacketListener#processPacket(
     *     org.jivesoftware.smack.packet.Packet)
     */
    public void processPacket(Packet packet) {
        if (packet instanceof Message) {
            Date now = new Date(System.currentTimeMillis() - 1000);
            Date date = new Date();
            PacketExtension delay = packet.getExtension("x", "jabber:x:delay");
            if ((delay != null) && (delay instanceof DelayInformation)) {
                DelayInformation delayinfo = (DelayInformation) delay;
                date = delayinfo.getStamp();
            }
            if (ignorePastMessages && date.before(now)){
                return;
            }
            Message message = (Message) packet;
            String body = message.getBody();
            String from = message.getFrom();
            addMessage(date, from, body);
        } else if (packet instanceof Presence) {
            Presence presence = (Presence) packet;
            Presence.Type type = presence.getType();
            String from = packet.getFrom();
            from = from.replace(groupName, "");
            if (from.startsWith("/")) {
                from = from.substring(1);
            }
            if (type != null) {
                if (type == Presence.Type.AVAILABLE) {
                    synchronized (roster) {
                        if (!roster.contains(from)) {
                            roster.add(from);
                            addRoster(from);
                            rosterChanged = true;
                            roster.notify();
                        }
                    }
                } else if (type == Presence.Type.UNAVAILABLE) {
                    synchronized (roster) {
                        if (roster.contains(from)) {
                            roster.remove(from);
                            removeRoster(from);
                            rosterChanged = true;
                            roster.notify();
                        }
                    }
                }
            }
        }
    }

    /**
     * Waits until the messages are available and then gets them
     * @return The messages in the queue
     */
    public String getMessage() {
        lastAccess = System.currentTimeMillis();
        return getMessages();
    }

    /**
     * Closes the connection to the server
     */
    public void close() {
        done = true;
        addMessage(new Date(), "", "Disconnected *** ");
        synchronized (queue) {
            queue.notifyAll();
        }
        groupChat.leave();
        AccountManager manager = connection.getAccountManager();
        try {
            manager.deleteAccount();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        connection.close();
    }

    /**
     * Sends a message to the group
     * @param message The message to send
     */
    public void setMessage(String message) {
        try {
            groupChat.sendMessage(message);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the list of participants
     * @return the list of participants
     */
    public Vector<String> getRoster() {
        return roster;
    }

    /**
     * Waits for the roster to be changed
     * @return True if the roster was changed, false if the client was closed
     */
    public boolean isRosterChanged() {
        synchronized (roster) {
            while (!done && !rosterChanged) {
                try {
                    roster.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
        }
        if (rosterChanged) {
            rosterChanged = false;
            return true;
        }
        return false;
    }

    /**
     * Changes the nickname of the participant
     * @param newnickname The new nickname
     * @throws XMPPException
     */
    public void setNickname(String newnickname) throws XMPPException {
        groupChat.leave();
        ignorePastMessages = true;
        groupChat.join(newnickname.replace("'", "&apos;"));
    }

    /**
     * Gets the current nickname
     * @return the nickname
     */
    public String getNickname() {
        return groupChat.getNickname();
    }
}
