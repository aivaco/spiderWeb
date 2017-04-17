package com.spider;
import java.io.*;
import java.nio.charset.Charset;
/**
 * It manages all the functions related to the file that has to be created.
 */
public class PrintOnFile {
    /**
     * Creates the file urlsSemilla.txt in C path.
     */
    private File file;
    public void createFile()
    {
        try{
            file = new File(".//urlsSemilla.txt");
            if (file.createNewFile())
            {
                System.out.println("Archivo urlsSemilla.txt creado exitosamente");
            }
            else
            {
                System.out.println("Error al crear el archivo urlsSemilla.txt");
            }
        }catch(IOException exception){
            exception.printStackTrace();
        }
    }

    public void writeInFile(String url, String type){
        String data = url + " " + type + "\n";
        Charset.forName("UTF-8").encode(data);              //Sets the string to UTF-8.
        try {
            FileWriter writer = new FileWriter(file, true);
            writer.write(data);
            writer.flush();
            writer.close();
        }catch(IOException exception){
            exception.printStackTrace();
        }
    }
}
