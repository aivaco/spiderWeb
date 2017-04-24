package com.spider;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;

/**
 * It manages all the functions related to the file that has to be created.
 */
public class OnFile {

    private File file;

    /**
     * Creates the file with a respective name in local path.
     * @param name
     * @param type
     */
    public void createFile(String name, String type)
    {
        try{
            file = new File(".//"+name+"."+ type);
            if (file.createNewFile())
            {
                System.out.println("Archivo "+ name +".txt creado exitosamente");
            }
            else
            {
                System.out.println("Error al crear el archivo "+ name +".txt");
            }
        }catch(IOException exception){
            exception.printStackTrace();
        }
    }

    /**
     * It's only used for write in a file.
     * @param data
     */
    private void writeInFile(String data)
    {
        try {
            FileWriter writer = new FileWriter(file, true);
            writer.write(data);
            writer.flush();
            writer.close();
        }catch(IOException exception){
            exception.printStackTrace();
        }
    }

    /**
     * It will be use to write the url and the document type in urls.txt file.
     * @param url
     * @param type
     */

    public void writeUrlInFile(String url, String type){
        String data = url + " " + type + "\n";
        Charset.forName("UTF-8").encode(data);              //Sets the string to UTF-8.
        writeInFile(data);
    }

    /**
     * Writes a line in a specific file.
     * @param data
     */
    public void writeLineInFile(String data)
    {
        data = data + "\n";
        Charset.forName("UTF-8").encode(data);              //Sets the string to UTF-8.
        writeInFile(data);
    }

    /**
     * Downloads the respective file.
     * @param link
     */
    public void downloadDocument(URL link)
    {
        try {
            FileUtils.copyURLToFile(link, file);
        }catch(IOException exception){
            exception.printStackTrace();
        }
    }

}
