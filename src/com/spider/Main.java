package com.spider;
public class Main {

    public static void main(String[] args) {

        WebCrawler webCrawler = new WebCrawler();
        webCrawler.loadSeeds();
        //webCrawler.startThreads(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Integer.parseInt(args[2]));
        webCrawler.startThreads(300,2,5);

        //Spider a = new Spider();
        //a.extractsURLs("http://www.ebay.com");
        //a.manageUrl("http://www.pdf995.com/samples/pdf.pdf");
        //a.getRobotTxt("https://www.facebook.com/");
    }
}
