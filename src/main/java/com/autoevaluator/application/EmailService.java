package com.autoevaluator.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

//    //Gmail logic
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("faizanfarooq56g@gmail.com"); // use your email here
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }


//    public void sendSimpleMail(String toEmail, String subject, String messageBody) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom("8f0513001@smtp-brevo.com"); // Must match your Brevo login
//        message.setTo(toEmail);
//        message.setSubject(subject);
//        message.setText(messageBody);
//
//        mailSender.send(message);
//    }
}