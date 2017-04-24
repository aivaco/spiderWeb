package com.spider;
public class Main {

    public static void main(String[] args) {
        Spider a = new Spider();
        a.extractsUrls("http://www.ebay.com");
        a.manageUrl("http://www.pdf995.com/samples/pdf.pdf");
        a.getRobotTxt("https://www.facebook.com/");
    }
}
