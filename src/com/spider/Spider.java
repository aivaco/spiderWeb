package com.spider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
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

    /*Shared variables*/
    private static int count = 0;                                                          //Contains the number of files that have been downloaded.
    private static HashMap foundURLs = new HashMap();                                      //Contains the urls that have been extracted from visited pages.
    private static List<String> toVisitURLs;                                              //Contains the urls that are going to be visited.
    private static HashSet visitedURLs = new HashSet();                                   //Contains the pages that had been visited.
    private static int current_size = 0;                                                  //Contains the size of the downloaded data.

    private static int currentLevel = 0;
    private static int max_level; //= 5;                                                        //Controls the quantity of levels that the spider could go deep.

    //private static int currentDocuments = 0;
    private static int max_documents; //= 100;                                                 //Contains the maximum files that could be downloaded.

    private static Semaphore mutex_current_size = new Semaphore(1);                     //Controls the access to current_size.
    private static Semaphore mutex_foundURLs = new Semaphore(1);                        //Controls the access to foundURLs hashset.
    private static Semaphore mutex_count = new Semaphore(1);                            //Controls the access to count.
    private static Semaphore mutex_visitedURLs = new Semaphore(1);
    private static Semaphore mutex_current_level = new Semaphore(1);

    private static final int NUM_THREADS = 4;                                                    // Contains the total number of threads
    private static CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS);                       //Establishes a barrier that doesn't allow threads to keep working until all of them reach the limit.

    private static int size_toVisitURLs = 0;            //Contains the size of the toVisitURLs list

    private static int firstThread_UpperBound = 0;     //Contains the Upper Bound of the toVisitURLs sublist that belongs to Thread #1
    private static int secondThread_UpperBound = 0;    //Contains the Upper Bound of the toVisitURLs sublist that belongs to Thread #2
    private static int thirdThread_UpperBound = 0;     //Contains the Upper Bound of the toVisitURLs sublist that belongs to Thread #3
    private static int fourthThread_UpperBound = 0;    //Contains the Upper Bound of the toVisitURLs sublist that belongs to Thread #4


    /*Local variables of each thread*/
    private String threadName;                                                      //Contains the name that identifies each thread

    private URL main_website;                                                       //Contains the url of the website.
    private int max_size; //= 10000 * 1024;                                         //Maximum size that could be downloaded by the crawler.
    private String protocol = "";                                                   //Stores the url protocol.
    private HashMap deniedUrls = new HashMap();                                     //Contains the disallowed urls by robots.txt.

    private int toVisit_fromIndex = 0;                                              //Variable that represents the first element of the of the toVisitURLs sublist that each thread will use
    private int toVisit_toIndex = 0;                                                //Variable that represents the last element of the of the toVisitURLs sublist that each thread will use

    private List<String> thread_toVisitURLs;

    private boolean alreadyVisited = false;
    private boolean restore_data = false;                                                 //It is used to check if the information has to be restored from the backup.
    private static List<String> downloadedURLs;                                              //Contains the urls that has been downloaded.

    public Spider(String threadName, List<String> seed, int max_size, int max_documents, int max_level, boolean restore_data) {
        this.threadName = threadName;
        this.toVisitURLs = new ArrayList<>(seed);
        this.max_size = max_size;
        this.max_documents = max_documents;
        this.max_level = max_level;
        this.restore_data = restore_data;
        this.downloadedURLs = new ArrayList<String>();
        System.out.println(threadName + " " + toVisitURLs);
    }

    @Override
    public void run() {

        if ("Spider1" == this.threadName && restore_data){
            restore();
        }
       /*Makes all the threads wait for the others*/
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        while (conditions()) {
            boolean seed = true;
            changeLevel(seed);  //Splits the seed into different sublist
            seed = false;

            try {
                /*Iterates over all the URLs in thread_toVisitURLs*/
                for (Iterator<String> itr = thread_toVisitURLs.iterator(); itr.hasNext(); ) {
                /*Checks if the URL has already been visited*/
                    String URL = itr.next();
                    System.out.println(threadName + " " + URL);

                    mutex_visitedURLs.acquire();
                    alreadyVisited = visitedURLs.add(URL);
                    mutex_visitedURLs.release();

                    if (alreadyVisited) {
                    /*If it hasn't been visited then it tries to download the file*/
                        if (!manageUrl(URL)) {
                            System.out.println(URL + " no se descargó.");
                        }
                        /*Removes the URL from the thread_toVisitURLs list*/
                        itr.remove();

                        /*Checks if the currentLevel isn't the last level*/
                        try {
                            mutex_current_level.acquire();
                            if (max_level >= currentLevel) {
                                /*If it isn't the last level then it extracts the URLs in the file*/
                                extractsURLs(URL);
                            }
                            mutex_current_level.release();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                /*Makes all the threads wait for the others*/
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            } finally {

                changeLevel(seed);

            }

        }

    }

    /**
     * It is used to control the repetitions of the process.
     * @return condition
     */
    public boolean conditions()
    {
        boolean condition = true;
        try {
            mutex_current_level.acquire();
            if (currentLevel > max_level) {
                condition = false;
            }
            mutex_current_level.release();
            mutex_current_size.acquire();
            if (current_size > max_size && condition) {
                condition = false;
            }
            mutex_current_size.release();
            mutex_count.acquire();
            if (count > max_documents && condition) {
                condition = false;
            }
            mutex_count.release();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return condition;
    }


    /**
     * Handles the level change when all the URLs in the current level have been visited
     */
    public void changeLevel(boolean seed) {
        /*Checks if the current thread is Spider1*/
        if ("Spider1" == this.threadName) {
            /*Current thread is Spider1*/

            if (!seed){
                toVisitURLs = new ArrayList<String>(foundURLs.values());    //Turns the foundURLs in the new toVisitURLs list
                OnFile newFile = new OnFile();
                for (String a : downloadedURLs) {
                    newFile.writeInFile(a);
                }
                downloadedURLs.clear();
            }
            if (!downloadedURLs.isEmpty()){

            }
            foundURLs.clear();                                          //Removes the values in the foundURLs list
            backup();
            firstThread_UpperBound = toVisitURLs.size() / 4;
            secondThread_UpperBound = firstThread_UpperBound * 2;
            thirdThread_UpperBound = firstThread_UpperBound * 3;
            fourthThread_UpperBound = toVisitURLs.size();
            try{
                mutex_current_level.acquire();
                ++currentLevel;
                mutex_current_level.release();
            }catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        try {
            /*Makes all the threads wait for the others*/
            barrier.await();


            if (this.threadName.equals("Spider1")) {
                toVisit_toIndex = firstThread_UpperBound;
            } else if (this.threadName.equals("Spider2")) {
                toVisit_fromIndex = firstThread_UpperBound;
                toVisit_toIndex = secondThread_UpperBound;
            } else if (this.threadName.equals("Spider3")) {
                toVisit_fromIndex = secondThread_UpperBound;
                toVisit_toIndex = thirdThread_UpperBound;
            } else if (this.threadName.equals("Spider4")) {
                toVisit_fromIndex = thirdThread_UpperBound;
                toVisit_toIndex = fourthThread_UpperBound;
            }

            /*Creates the sublist with the URLs that each thread will check in the next level*/

            this.thread_toVisitURLs = new ArrayList<>(toVisitURLs.subList(toVisit_fromIndex, toVisit_toIndex));
            barrier.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        System.out.println("CAMBIO DE NIVEL");
    }


    /**
     * Connects to a website and gets all the urls of the document.
     *
     * @param url
     */
    public void extractsURLs(String url) {
        boolean exists_robots = false;                                                                     //It's false if there is not robots.txt in the site that is visiting.
        //Extracts all the urls that aren't allowed to be visited.
        if (!deniedUrls.containsValue("/")) {                                                        //If it's possible to explore the website.
            if (url.contains("https")) {                                                                   //Checks the url protocol and save it in protocol variable.
                protocol = "https://";
            } else {
                protocol = "http://";
            }
            try {
                URL site = new URL(url);

                exists_robots = getRobotTxt(site.getAuthority());

                main_website = new URL(url);
                InputStream temporal = null;
                DataInputStream temporal_data;
                String information;

                temporal = main_website.openStream();
                temporal_data = new DataInputStream(new BufferedInputStream(temporal));                         //Converts the InputStream (bytes chain) into a DataInputStream. This allow to manipulate the data as a java primitive data type.
                BufferedReader temporal_buffer = new BufferedReader(new InputStreamReader(temporal_data));      //Converts DataInputStream to a BufferedReader (this is for allow to read correctly all the characters from the stream).

                Pattern p = Pattern.compile("href=\"(https|http)?://(.*?)\"");
                Matcher matcher;                                                                                //Will be used to apply the pattern to a string.
                String temp;                                                                                    //Stores the url without all the other characters from the line.

                while ((information = temporal_buffer.readLine()) != null) {                                    //Copies line by line all the information received by the URL class.
                    matcher = p.matcher(information);
                    if (matcher.find()) {
                        temp = cleanUrl(matcher.group());
                        if (!foundURLs.containsValue(temp)) {                                                   //Checks if is not repeated.
                            if (exists_robots) {
                                if (!searchInRobots(temp)) {                                                           //Checks if is allowed by robots.txt
                                    mutex_foundURLs.acquire();
                                    foundURLs.put(cleanUrl(matcher.group()), cleanUrl(matcher.group()));
                                    mutex_foundURLs.release();
                                }
                            } else {
                                mutex_foundURLs.acquire();
                                foundURLs.put(cleanUrl(matcher.group()), cleanUrl(matcher.group()));
                                mutex_foundURLs.release();
                            }
                        }

                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes all other information of the string that it's not necessary.
     *
     * @param information
     * @return
     */
    public String cleanUrl(String information) {
        return fromTheSameWebsite(information.substring(information.indexOf('"') + 1, information.lastIndexOf('"')));
    }

    /**
     * It's in charge of classify the URLs.
     *
     * @param url
     */
    public boolean manageUrl(String url) {

        boolean success = false;
        try {
            URL link = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)  link.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setReadTimeout(5*1000);
            connection.connect();
            String content_type = connection.getContentType();
            connection.disconnect();
            if(connection.getContentType() == null){
                content_type = "text/html";
            }
            if (content_type.contains("text/html")) {
                success = downloadDocument(fromTheSameWebsite(url), "html");
            } else if (content_type.contains("text/htm")) {
                success = downloadDocument(fromTheSameWebsite(url), "htm");
            } else if (content_type.contains("application/xhtml+xml")) {
                success = downloadDocument(fromTheSameWebsite(url), "xhtml");
            } else if (content_type.contains("application/pdf")) {
                success = downloadDocument(fromTheSameWebsite(url), "pdf");
            } else if (content_type.contains("application/msword")) {
                success = downloadDocument(fromTheSameWebsite(url), "doc");
            } else if (content_type.contains("application/mswordapplication/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                success = downloadDocument(fromTheSameWebsite(url), "docx");
            } else if (content_type.contains("application/vnd.oasis.opendocument.text ")) {
                success = downloadDocument(fromTheSameWebsite(url), "odt");
            } else if (content_type.contains("text/plain")) {
                success = downloadDocument(fromTheSameWebsite(url), "txt");
            } else if (content_type.contains("application/rtf.rtf")) {
                success = downloadDocument(fromTheSameWebsite(url), "rtf");
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return success;
    }

    /**
     * It's in charge of download the different documents that could be found by the crawler.
     *
     * @param url
     * @param type
     */
    public boolean downloadDocument(String url, String type) {
        boolean success = false;
        try {
            URL page = new URL(url);
            OnFile newFile = new OnFile();                                                                 //Creates a new file.

            mutex_count.acquire();
            newFile.createFile(Integer.toString(count), "");                                             //Assigns the respective number of the document to the name of the file.
            ++count;
            mutex_count.release();

            mutex_current_size.acquire();
            current_size = newFile.downloadDocument(page, current_size, max_size);
            //newFile.writeUrlInFile(url,type);
            String data = url + " " + type + "\n";
            Charset.forName("UTF-8").encode(data);
            downloadedURLs.add(data);
            mutex_current_size.release();

            success = true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return success;
    }

    /**
     * Checks if a URL is in complete form. If not, it going to complete the url with the authority of the main website.
     *
     * @param url
     * @return
     */
    public String fromTheSameWebsite(String url) {
        if (url.contains("http")) {
            return url;
        } else if (url.contains("www")) {
            return url;
        } else {
            return protocol + main_website.getAuthority() + "/" + url;
        }
    }

    /**
     * Gets the sections where the crawler can not enter.
     *
     * @param link
     * @return
     */
    public boolean getRobotTxt(String link) {

        boolean robotsExists = true;
        URL url;
        try {
            boolean crawler = false;                                                   //Checks if exist some limitations for the crawler.
            url = new URL(protocol + link + "/robots.txt");
            InputStream temporal = null;
            DataInputStream temporal_data;
            String information;
            //temporal = url.openStream();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5*1000);
            if (200 != conn.getResponseCode()) {                //Returns an exception if it does a getInputStream and robots.txt file doesn't exists.
                System.out.println(url + " robots.txt NO EXISTE");
                robotsExists = false;
            } else {
                temporal = new BufferedInputStream(conn.getInputStream());
                temporal_data = new DataInputStream(new BufferedInputStream(temporal));                         //Converts the InputStream (bytes chain) into a DataInputStream. This allow to manipulate the data as a java primitive data type.
                BufferedReader temporal_buffer = new BufferedReader(new InputStreamReader(temporal_data));     //Converts DataInputStream to a BufferedReader (this is for allow to read correctly all the characters from the stream).
                String temp;
                //conn.disconnect();

                while (((information = temporal_buffer.readLine()) != null)) {                         //Copies line by line all the information received by the URL class.
                    if (information.contains("User-agent: *")) {
                        crawler = true;
                    } else if (crawler) {
                        if (information.contains("Disallow: ")) {
                            temp = information.substring(information.indexOf(' '), information.length());
                            deniedUrls.put(temp, temp);
                        }
                    }

                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return robotsExists;
    }

    /**
     * Looks for a restriction with the respective url.
     *
     * @param url
     * @return
     */
    private boolean searchInRobots(String url) {
        boolean stop = false;
        Set set = deniedUrls.entrySet();            // Gets a set of the entries
        Iterator i = set.iterator();                // Gets an iterator
        String temp;
        while (!stop && i.hasNext()) {               // Iterates elements
            Map.Entry me = (Map.Entry) i.next();
            temp = (String) me.getValue();
            if (temp.contains("*")) {
                temp = temp.substring(temp.indexOf('*') + 1, temp.length());
            }
            stop = url.contains(temp);
        }
        return stop;
    }

    /**
     * Backups the information of the urls that are visited by the crawler.
     */
    private void backup()
    {

        OnFile backup_file = new OnFile();
        backup_file.clearFile("backup_visited", "txt");
        backup_file.createFile("backup_visited","txt");
        for (Object link : visitedURLs)
        {
            backup_file.writeLineInBackUpFile(link.toString(), "backup_visited");
        }

        OnFile backup_file_2 = new OnFile();
        backup_file_2.clearFile("backup_toVisit", "txt");
        backup_file_2.createFile("backup_toVisit","txt");
        for (Object link : toVisitURLs)
        {
            backup_file_2.writeLineInBackUpFile(link.toString(), "backup_toVisit");
        }

        OnFile parameters = new OnFile();
        parameters.clearFile("parameters", "txt");
        parameters.createFile("parameters", "txt");
        parameters.writeLineInBackUpFile(Integer.toString(max_size),"parameters");
        parameters.writeLineInBackUpFile(Integer.toString(max_documents),"parameters");
        parameters.writeLineInBackUpFile(Integer.toString(max_level),"parameters");
        parameters.writeLineInBackUpFile(Integer.toString(current_size),"parameters");
        parameters.writeLineInBackUpFile(Integer.toString(count),"parameters");
        parameters.writeLineInBackUpFile(Integer.toString(currentLevel),"parameters");

    }


    /**
     * Restores the information of an execution.
     */

    private void restore()
    {
        OnFile file = new OnFile();
        toVisitURLs = file.readFile(".//backup_toVisit.txt");
        visitedURLs = file.readFileHash(".//backup_visited.txt");
        int[] parameters = file.readParameters(".//parameters.txt");
        max_size = parameters[0];
        max_documents = parameters[1];
        max_level = parameters[2];
        current_size = parameters[3];
        count = parameters[4];
        currentLevel = parameters[5];
    }

}
