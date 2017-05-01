package com.spider;
public class Main {

    public static void main(String[] args) {

        WebCrawler webCrawler = new WebCrawler();
        webCrawler.loadSeeds();
        webCrawler.startThreads();

        //Spider a = new Spider();
        //a.extractsURLs("http://www.ebay.com");
        //a.manageUrl("http://www.pdf995.com/samples/pdf.pdf");
        //a.getRobotTxt("https://www.facebook.com/");
    }
}
