package com.spider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * It will manage all the functions of the spider bot.
 */
public class Spider {

    private int count = 0;                                                          //Contains the number of files that have been downloaded.
    //private List<String> extractedUrls = new ArrayList<String>();
    HashSet extractedUrls = new HashSet();                                          //Contains the urls that have been extracted from visited pages. HashSet doesn't allow duplicates.
    private URL main_website;                                                       //Contains the url of the website.
    private int current_size = 0;                                                   //Contains the size of the downloaded data.
    private int max_size = 1000000000;                                             //Maximum size that could be downloaded by the crawler.
    private String protocol = "";                                                  //Stores the url protocol.
    /**
     * Connects to a website and gets all the urls of the document.
     * @param url
     */
    public void extractsUrls(String url)
    {
        if(url.contains("https")){                                                  //Checks the url protocol and save it in protocol variable.
            protocol = "https://";
        }
        else{
            protocol = "http://";
        }
        //List<String> containedUrls = new ArrayList<String>();
        try {
//            System.out.println(u.getAuthority());                                //Returns the url in a simple form. Example: www.google.com.
//            System.out.println(u.getRef());                                      //Returns the rest of the URL. Example: /home/theory.txt.
//            System.out.println(u.getFile());                                     //Returns the rest of the URL. Example: /home/theory.txt.
            main_website = new URL(url);
            InputStream temporal = null;
            DataInputStream temporal_data;
            String information;
            temporal = main_website.openStream();
            temporal_data = new DataInputStream(new BufferedInputStream(temporal));                         //Converts the InputStream (bytes chain) into a DataInputStream. This allow to manipulate the data as a java primitive data type.
            BufferedReader temporal_buffer = new BufferedReader(new InputStreamReader(temporal_data));     //Converts DataInputStream to a BufferedReader (this is for allow to read correctly all the characters from the stream).
            //OnFile newFile = new OnFile();                                                                 //Creates a new file.
            //newFile.createFile(Integer.toString(count),"txt");                                                   //Assigns the respective number of the document to the name of the file.
//            Pattern p = Pattern.compile(                                                                   //Pattern that will be used to recognize urls.
//                    "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
//                            "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
//                            "|mil|biz|info|mobi|name|aero|jobs|museum" +
//                            "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
//                            "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
//                            "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
//                            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
//                            "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" +
//                            "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
//                            "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");
            Pattern p = Pattern.compile("href=\"(.*?)\"");
            Matcher matcher;                                                                            //Will be used to apply the pattern to a string.
            String temp;                                                                                //Stores the url without all the other characters from the line.

            while ((information = temporal_buffer.readLine()) != null) {                                //Copies line by line all the information received by the URL class.
                //newFile.writeLineInFile(information);
                matcher = p.matcher(information);
              if(matcher.find()) {
                  extractedUrls.add(cleanUrl(matcher.group()));
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Esto hay que borrarlo
        //for (String hi : extractedUrls)
        //    System.out.println(hi);
    }

    /**
     * Deletes all other information of the string that it's not necessary.
     * @param information
     * @return
     */
    public String cleanUrl(String information){
        return fromTheSameWebsite(information.substring(information.indexOf('"')+1, information.lastIndexOf('"')));
    }

    /**
     * It's in charge of classify the URLs.
     * @param url
     */
    public void manageUrl(String url) {

        if(url.contains(".html")){
            downloadDocument(fromTheSameWebsite(url),"html");
        }
        else if (url.contains(".htm")){
            downloadDocument(fromTheSameWebsite(url),"htm");
        }
        else if (url.contains(".xhtml")){
            downloadDocument(fromTheSameWebsite(url),"xhtml");
        }
        else if (url.contains(".pdf")){
            downloadDocument(fromTheSameWebsite(url),"pdf");
        }
        else if (url.contains(".doc")){
            downloadDocument(fromTheSameWebsite(url),"doc");
        }
        else if (url.contains(".docx")){
            downloadDocument(fromTheSameWebsite(url),"docs");
        }
        else if (url.contains(".odt")){
            downloadDocument(fromTheSameWebsite(url),"odt");
        }
        else if (url.contains(".txt")){
            downloadDocument(fromTheSameWebsite(url),"txt");
        }
        else if (url.contains(".rtf")){
            downloadDocument(fromTheSameWebsite(url),"rtf");
        }

    }

    /**
     * It's in charge of download the different documents that could be found by the crawler.
     * @param url
     * @param type
     */
    public void downloadDocument(String url, String type)
    {
        try {
            URL page = new URL(url);
            OnFile newFile = new OnFile();                                                                 //Creates a new file.
            newFile.createFile(Integer.toString(count), type);                                             //Assigns the respective number of the document to the name of the file.
            newFile.downloadDocument(page, current_size,max_size);
        }
     catch (MalformedURLException e) {
        e.printStackTrace();
     } catch (IOException e) {
        e.printStackTrace();
        }
    }

    /**
     * Checks if a URL is in complete form. If not, it going to complete the url with the authority of the main website.
     * @param url
     * @return
     */
    public String fromTheSameWebsite(String url){
        if(url.contains("http"))
        {
            return url;
        }
        else if(url.contains("www")){
            return url;
        }
        else {
            return protocol+main_website.getAuthority()+"/"+url;
        }
    }

    /**
     * Gets the sections where the crawler can not enter.
     * @param link
     * @return
     */
    public List<String> getRobotTxt(String link){
        List<String> deniedUrls = new ArrayList<String>();
        URL url;
        try{
            boolean crawler = false;                                                   //Checks if exist some limitations for the crawler.
            url = new URL(link+"robots.txt");
            InputStream temporal = null;
            DataInputStream temporal_data;
            String information;
            temporal = url.openStream();
            temporal_data = new DataInputStream(new BufferedInputStream(temporal));                         //Converts the InputStream (bytes chain) into a DataInputStream. This allow to manipulate the data as a java primitive data type.
            BufferedReader temporal_buffer = new BufferedReader(new InputStreamReader(temporal_data));     //Converts DataInputStream to a BufferedReader (this is for allow to read correctly all the characters from the stream).

            while (((information = temporal_buffer.readLine()) != null)) {                         //Copies line by line all the information received by the URL class.
               if (information.contains("User-agent: *")){
                   crawler = true;
               }
               else if(crawler){
                   if(information.contains("Disallow: ")){
                       deniedUrls.add(information.substring(information.indexOf(' '), information.length()));
                   }
               }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Esto hay que borrarlo
        for (String hi : deniedUrls)
            System.out.println(hi);
        return deniedUrls;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
    public int getCurrent_size() {
        return current_size;
    }

    public void setCurrent_size(int current_size) {
        this.current_size = current_size;
    }

    public int getMax_size() {
        return max_size;
    }

    public void setMax_size(int max_size) {
        this.max_size = max_size;
    }
}
