package com.spider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import static java.lang.Math.toIntExact;

/**
 * It manages all the functions related to the file that has to be created.
 */
public class OnFile {

    private File file;

    /**
     * Creates the file with a respective name in local path.
     *
     * @param name
     * @param type
     */
    public void createFile(String name, String type) {
        try {
            file = new File(".//" + name + "." + type);
            if (file.createNewFile()) {
                System.out.println("Archivo " + name + "." + type + " creado exitosamente");
            } else {
                System.out.println("Error al crear el archivo " + name + "." + type);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * It's only used for write in a file.
     *
     * @param data
     */
    private void writeInFile(String data) {
        try {
            FileWriter writer = new FileWriter(file, true);
            writer.write(data);
            writer.flush();
            writer.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * It will be use to write the url and the document type in urls.txt file.
     *
     * @param url
     * @param type
     */

    public void writeUrlInFile(String url, String type) {
        String data = url + " " + type + "\n";
        Charset.forName("UTF-8").encode(data);              //Sets the string to UTF-8.
        writeInFile(data);
    }

    /**
     * Writes a line in a specific file.
     *
     * @param data
     */
    public void writeLineInFile(String data) {
        data = data + "\n";
        Charset.forName("UTF-8").encode(data);              //Sets the string to UTF-8.
        writeInFile(data);
    }

    /**
     * Downloads the respective file. //If the size of the downloaded file exceeds the maximum size allowed, the file will be erased.(borrar)//
     *
     * @param link
     */
    public int downloadDocument(URL link, int current_size, int max_size) {
        int file_size = 0;                                      //Contains the actual size of the downloaded file.
        if (current_size < max_size) {
            try {
                FileUtils.copyURLToFile(link, file);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            file_size = toIntExact(FileUtils.sizeOf(file));
            System.out.println("El archivo: " + file.getName() + " pesa " + file_size + " bytes.");

//            if (current_size + file_size < max_size)
//            {
//                file.delete();
//            }
        } else {
            System.out.println("Imposible descargar más información, el límite es de: " + max_size + "bytes.");
        }

        return current_size + file_size;
    }

    /**
     * Reads a file and put line by line in a list.
     *
     * @param name
     * @return
     */
    public List<String> readFile(String name) {

        List<String> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                list.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

}
