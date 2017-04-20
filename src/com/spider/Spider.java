package com.spider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * It will manage all the functions of the spider bot.
 */
public class Spider {

    private int count = 0;

    /**
     * Connects to an url, gets the content of the document and write it to a file.
     * @param url
     */
    public void openURL(String url)
    {
        URL site;
        try {

//            System.out.println(u.getAuthority());                                //Returns the url in a simple form. Example: www.google.com.
//            System.out.println(u.getRef());                                      //Returns the rest of the URL. Example: /home/theory.txt.
//            System.out.println(u.getFile());                                     //Returns the rest of the URL. Example: /home/theory.txt.
            site = new URL(url);
            InputStream temporal = null;
            DataInputStream temporal_data;
            String information;
            temporal = site.openStream();
            temporal_data = new DataInputStream(new BufferedInputStream(temporal));                         //Converts the InputStream (bytes chain) into a DataInputStream. This allow to manipulate the data as a java primitive data type.
            BufferedReader temporal_buffer = new BufferedReader(new InputStreamReader(temporal_data));     //Converts DataInputStream to a BufferedReader (this is for allow to read correctly all the characters from the stream).
            OnFile newFile = new OnFile();                                                                 //Creates a new file.
            newFile.createFile(Integer.toString(count));                                                   //Assign the respective number of the document to the name of the file.
            while ((information = temporal_buffer.readLine()) != null) {                                   //Copy line by line all the information received by the URL class.
                newFile.writeLineInFile(information);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
