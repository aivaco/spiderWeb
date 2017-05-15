package com.spider;


public class Main {

    public static void main(String[] args) {

        WebCrawler webCrawler = new WebCrawler();
        webCrawler.loadSeeds();
        //webCrawler.startThreads(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Integer.parseInt(args[2]));
        webCrawler.startThreads(300,500, 5,false);

    }
}
