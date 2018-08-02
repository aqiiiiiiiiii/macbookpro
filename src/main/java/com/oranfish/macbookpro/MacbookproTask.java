package com.oranfish.macbookpro;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class MacbookproTask {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromUserName;
    @Value("${spring.mail.receive.usernames}")
    private String toUserNames;
    @Value("${task.url}")
    private String url;
    @Value("${task.year}")
    private int year;
    @Value("${task.frequency}")
    private double frequency;
    @Value("${task.price}")
    private int price;

    private boolean finded = false;



    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void run(){
        while(!finded){
            runTask();
            try {
                Thread.sleep(60*10*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void runTask(){
        String htmlContent = HttpUtils.getPageContent(url);
        if(htmlContent == null){
            return;
        }
        Document document = Jsoup.parse(htmlContent);
        Elements tables = document.select("#primary div div[class=box-content] table");
        List<Element> qualifiedList = new ArrayList<>();
        for(Element e : tables){
            if(validate(e)){
                qualifiedList.add(e);
            }
        }
        if(!CollectionUtils.isEmpty(qualifiedList)){
            sendEmail(qualifiedList);
        }
        System.out.println(sdf.format(new Date()) + ", success!");
    }

    public boolean validate(Element e){
        boolean yearValidate = false;
        boolean frequencyValidate = false;
        boolean priceValidate = false;
        Element specs = e.select("tbody tr td[class=specs]").get(0);
        Element priceElement = e.select("tbody tr td[class=purchase-info] p[class=price] span[class=current_price]").get(0);

        String specsText = specs.text().replaceAll(" ", "");
        String yearStr = specsText.substring(specsText.indexOf("最初发布于")+5, specsText.indexOf("最初发布于")+9);
        String frequencyStr = specsText.substring(18, 21);
        String priceStr = priceElement.text().replaceAll(" ", "").replaceAll(",", "").replaceAll("RMB", "");

        if(Integer.parseInt(yearStr) == year){
            yearValidate = true;
        }
        if(Double.parseDouble(frequencyStr) == frequency){
            frequencyValidate = true;
        }
        if(Integer.parseInt(priceStr) < price){
            priceValidate = true;
        }
        if(yearValidate && frequencyValidate && priceValidate){
            return true;
        }else{
            return false;
        }
    }

    public void sendEmail(List<Element> qualifiedList){
        MimeMessage message = null;
        try {
            message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromUserName);
            String[] toUsers = toUserNames.split(",");
            helper.setTo(toUsers);
            helper.setSubject("macbook pro");

            StringBuffer sb = new StringBuffer();
            for(Element e : qualifiedList){
                sb.append(e.html()) ;
            }
            helper.setText(sb.toString(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mailSender.send(message);
        System.out.println(sdf.format(new Date()) + ", finded!");
        finded = true;
    }
}
