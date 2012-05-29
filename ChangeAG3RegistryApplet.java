/*
 * @(#)ChangeAG3RegistryApplet.java
 * Created: 17-Jan-2007
 * Version: 1.0
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Changes an AG3 Bridge Registry
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ChangeAG3RegistryApplet extends JApplet implements ActionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = -3493174422110761749L;

	// The registry value
    private JTextField registry = new JTextField();

    // The change button
    private JButton change = new JButton("Change");

    // A list of the lines in the file
    private Vector<String> lines = new Vector<String>();

    // The set of preferences
    private Properties preferences = new Properties();

    // The file to write
    private String file = null;

    // Null if there is no error
    private String error = null;

    /**
     *
     * @see java.applet.Applet#init()
     */
    public void init() {
        String slash = System.getProperty("file.separator");
        String os = System.getProperty("os.name").toLowerCase();
        file = System.getProperty("user.home") + slash;
        if (os.indexOf("windows") != -1) {
            file += "Application Data" + slash;
        } else {
            file += ".";
        }
        file += "AccessGrid3" + slash + "Config" + slash + "preferences";
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    BufferedReader reader =
                        new BufferedReader(new FileReader(file));
                    String line = reader.readLine();
                    while (line != null) {
                        lines.add(line);
                        int split = line.indexOf(" = ");
                        if (split != -1) {
                            String item = line.substring(0, split);
                            String value = line.substring(split + 3);
                            preferences.setProperty(item, value);
                        }
                        line = reader.readLine();
                    }
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    error = e.getMessage();
                }
                return null;
            }
        });
        if (error == null) {
            change.addActionListener(this);
            registry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            getContentPane().setLayout(new BoxLayout(getContentPane(),
                    BoxLayout.X_AXIS));
            getContentPane().add(registry);
            getContentPane().add(change);
        } else {
            getContentPane().add(new JLabel("Error: " + error));
        }
    }

    /**
     *
     * @see java.applet.Applet#start()
     */
    public void start() {
        registry.setText(preferences.getProperty("bridgeRegistry"));
    }

    /**
     *
     * @see java.awt.event.ActionListener#actionPerformed(
     *     java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        preferences.setProperty("bridgeRegistry", registry.getText());
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    PrintWriter writer = new PrintWriter(new FileWriter(file));
                    for (int i = 0; i < lines.size(); i++) {
                        String line = (String) lines.get(i);
                        int split = line.indexOf(" = ");
                        if (split != -1) {
                            String item = line.substring(0, split);
                            String value = line.substring(split + 3);
                            if (preferences.containsKey(item)) {
                                value = preferences.getProperty(item);
                            }
                            writer.println(item + " = " + value);
                        } else {
                            writer.println(line);
                        }
                    }
                    writer.close();
                    JOptionPane.showMessageDialog(null,
                            "The Registry has been updated.  "
                            + "Please restart any running AG3 clients.");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Error: " + e.getMessage());
                }
                return null;
            }
        });
    }
}
