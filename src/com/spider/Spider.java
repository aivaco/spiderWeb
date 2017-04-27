package com.spider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * It will manage all the functions of the spider bot.
 */
public class Spider implements Runnable {

    private int count = 0;                                                          //Contains the number of files that have been downloaded.
    private HashMap foundUrls;                                                      //Contains the urls that have been extracted from visited pages.
    private List<String> toVisitUrls;                                               //Contains the urls that are going to be visited.
    private HashSet visitedUrls = new HashSet();                                    //Contains the pages that had been visited.
    private URL main_website;                                                       //Contains the url of the website.
    private int current_size = 0;                                                   //Contains the size of the downloaded data.
    private int max_size = 0;                                                       //Maximum size that could be downloaded by the crawler.
    private String protocol = "";                                                   //Stores the url protocol.
    private HashMap deniedUrls = new HashMap();                                     //Contains the disallowed urls by robots.txt.
    private String url;                                                             //Contains the seed url.
    private static Semaphore mutex_visited = new Semaphore(1);               //Uses to control the access to visitedUrl.
    private static Semaphore mutex_current_size = new Semaphore(1);          //Uses to control the access to current_size.
    private CyclicBarrier barrier = new CyclicBarrier(3);                    //Establishes a barrier that don't allow threads to keep working until all of them reach the limit.

    public Spider(String url, int max_size){
        this.max_size = max_size;
        this.url = url;
        foundUrls = new HashMap();
        toVisitUrls = new ArrayList<String>();
    }

    @Override
    public void run()
    {
        String url_to_visit;
        extractsUrls(url);                                              //Extracts all the urls that it has to visit.
        //Downloads the file of the first url
        try {
            mutex_visited.acquire();
            if (!visitedUrls.contains(url)) {
                manageUrl(url);
                visitedUrls.add(url);
            }
            mutex_visited.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        toVisitUrls = (List<String>) foundUrls.values();                 //All the extracted urls pass to be part of the list: toVisitUrls.
        foundUrls.clear();
        while (current_size < max_size) {
            for (int i = 0; i < toVisitUrls.size(); ++i) {              //Starts to explore all the urls of one level.
                url_to_visit = toVisitUrls.get(i);
                extractsUrls(url_to_visit);
                System.out.println("Se exploró y se extrayeron los links de la url: " + url_to_visit);
                try {
                    mutex_visited.acquire();
                    if (!visitedUrls.contains(url_to_visit)) {            //Tries to visit all the extracted urls.
                        manageUrl(url_to_visit);
                        toVisitUrls.remove(i);
                        visitedUrls.add(url_to_visit);
                        System.out.println("Se descargó el archivo de la url: " + url_to_visit);
                    }
                    mutex_visited.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                barrier.await();
                toVisitUrls.clear();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Connects to a website and gets all the urls of the document.
     * @param url
     */
    public void extractsUrls(String url)
    {
        boolean exists_robots = false;                                                                     //It's false if there is not robots.txt in the site that is visiting.
        exists_robots = getRobotTxt(url);                                                                  //Extracts all the urls that aren't allowed to be visited.
        if (!deniedUrls.containsValue("/")) {                                                        //If it's possible to explore the website.
            if (url.contains("https")) {                                                                   //Checks the url protocol and save it in protocol variable.
                protocol = "https://";
            } else {
                protocol = "http://";
            }
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
                BufferedReader temporal_buffer = new BufferedReader(new InputStreamReader(temporal_data));      //Converts DataInputStream to a BufferedReader (this is for allow to read correctly all the characters from the stream).
                Pattern p = Pattern.compile("href=\"(.*?)\"");
                Matcher matcher;                                                                                //Will be used to apply the pattern to a string.
                String temp;                                                                                    //Stores the url without all the other characters from the line.

                while ((information = temporal_buffer.readLine()) != null) {                                    //Copies line by line all the information received by the URL class.
                    matcher = p.matcher(information);
                    if (matcher.find()) {
                        temp = cleanUrl(matcher.group());
                        if (!foundUrls.containsValue(temp)) {                                                   //Checks if is not repeated.
                            if(exists_robots) {
                                if (!searchInRobots(temp)){                                                           //Checks if is allowed by robots.txt
                                    foundUrls.put(cleanUrl(matcher.group()), cleanUrl(matcher.group()));
                                }
                            }
                            else {
                                foundUrls.put(cleanUrl(matcher.group()), cleanUrl(matcher.group()));
                            }
                        }

                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
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
            mutex_current_size.acquire();
            newFile.downloadDocument(page, current_size,max_size);
            mutex_current_size.release();
        }
     catch (MalformedURLException e) {
        e.printStackTrace();
     } catch (IOException e) {
        e.printStackTrace();
        } catch (InterruptedException e) {
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
    public boolean  getRobotTxt(String link){

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
            String temp;

            while (((information = temporal_buffer.readLine()) != null)) {                         //Copies line by line all the information received by the URL class.
               if (information.contains("User-agent: *")){
                   crawler = true;
               }
               else if(crawler){
                   if(information.contains("Disallow: ")){
                       temp = information.substring(information.indexOf(' '), information.length());
                       deniedUrls.put(temp,temp);
                   }
               }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Esto hay que borrarlo
//        for (String hi : deniedUrls)
//            System.out.println(hi);

//        // Get a set of the entries
//        Set set = deniedUrls.entrySet();
//
//        // Get an iterator
//        Iterator i = set.iterator();
//
//        // Display elements
//        while(i.hasNext()) {
//            Map.Entry me = (Map.Entry) i.next();
//            //System.out.print(me.getKey() + ": ");
//            System.out.println(me.getValue());
//        }
        return true;
    }

    /**
     * Looks for a restriction with the respective url.
     * @param url
     * @return
     */
    private boolean searchInRobots(String url)
    {
        boolean stop = false;
        Set set = deniedUrls.entrySet();            // Gets a set of the entries
        Iterator i = set.iterator();                // Gets an iterator
        String temp;
        while(!stop && i.hasNext()) {               // Iterates elements
            Map.Entry me = (Map.Entry) i.next();
            temp = (String)me.getValue();
            if(temp.contains("*")){
                temp = temp.substring(temp.indexOf('*')+1, temp.length());
            }
            stop = url.contains(temp);
        }
        return stop;
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
