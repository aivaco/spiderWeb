package com.spider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by jedups on 29/04/2017.
 */
public class WebCrawler {


    private List<String> toVisitURLs = new ArrayList<>();                               //Creates a List that will contain the URLs that are going to be visit in each level
    private HashMap foundURLs;                                      //Creates a Hashmap that will contain the URLs found by the Web Crawler in each
    private HashSet<String> visitedURLs;                            //Creates a Hashset that will contain the URLs that the Web Crawler has visited


    private CyclicBarrier barrier = new CyclicBarrier(3);        //Creates a barrier that will make the threads to wait until a third one arrives

    int level = 5;

    public void startThreads () {

        Spider firstSpider = new Spider("Spider1", toVisitURLs);
        Spider secondSpider = new Spider ("Spider2", toVisitURLs);
        Spider thirdSpider  = new Spider("Spider3", toVisitURLs);
        Spider fourthSpider = new Spider("Spider4", toVisitURLs);

        /*Creates the four threads that will crawl the URLs*/
        Thread firstThread = new Thread (firstSpider);
        Thread secondThread = new Thread (secondSpider);
        Thread thirdThread = new Thread (thirdSpider);
        Thread fourthThread = new Thread (fourthSpider);

        firstThread.start();
        secondThread.start();
        thirdThread.start();
        fourthThread.start();
    }

    public void loadSeeds () {
        OnFile file = new OnFile();
        toVisitURLs = file.readFile(".//urlsSemilla.txt");
    }

}
