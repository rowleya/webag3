/*
 * @(#)Launch.java
 * Created: 07-Sep-2006
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

package launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Launches an external application (native) packaged inside the classpath
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Launch {
    
    private File tempDir = null;
    
    /**
     * Extracts the given zip file in the class path into the user's home dir
     * @param file The zip file name in the class path
     * @throws IOException 
     */
    public void extract(String file) throws IOException {
        InputStream input = getClass().getResourceAsStream(file);
        ZipInputStream zipInput = new ZipInputStream(input);
        String home = System.getProperty("user.home");
        ZipEntry entry = null;
        tempDir = File.createTempFile("launch", "", new File(home));
        tempDir.delete();
        tempDir.mkdir();
        while ((entry = zipInput.getNextEntry()) != null) {
            byte[] bytes = new byte[8096];
            OutputStream output = new FileOutputStream(new File(tempDir, 
                    entry.getName()));
            int bytesRead = 0;
            while ((bytesRead = input.read(bytes)) != -1) {
                output.write(bytes, 0, bytesRead);
            }
            output.close();
        }
        zipInput.close();
        input.close();
    }
    
    /**
     * Removes the files extracted
     *
     */
    public void remove() {
        remove(tempDir);
    }
    
    private void remove(File dir) {
        File[] contents = dir.listFiles();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isFile()) {
                contents[i].delete();
            } else if (contents[i].isDirectory()) {
                remove(contents[i]);
            }
        }
    }
    
    /**
     * Creates a temporary file in the temporary directory of this launch
     * @param extension The extension to give the file
     * @return The created file
     * @throws IOException
     */
    public File getTempFile(String extension) throws IOException {
        return File.createTempFile("launch", extension, tempDir);
    }
    
    /**
     * Launches a file within the extracted directory
     * @param file The file to launch
     * @param args The arguments to the file, or null for none
     * @param envp The environment variables, or null to copy current ones
     * @param dir The working directory for the launch or null for extract dir
     * @throws IOException 
     */
    public void launch(String file, String[] args, String[] envp, File dir) 
            throws IOException {
        String[] arguments = null;
        File launchFile = new File(tempDir, file);
        if (args == null) {
            arguments = new String[1];
            arguments[0] = launchFile.getCanonicalPath();
        } else {
            arguments = new String[args.length + 1];
            System.arraycopy(args, 0, arguments, 1, args.length);
            arguments[0] = launchFile.getCanonicalPath();
        }
        if (dir == null) {
            dir = tempDir;
        }
        Runtime.getRuntime().exec(arguments, envp, dir);
    }
}
