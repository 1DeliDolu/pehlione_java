package com.pehlione.web.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    public void sendHtml(String to, String subject, String html) {
        // SMTP entegrasyonu gelene kadar reset akışını bloklamamak için logluyoruz.
        log.info("Mail queued: to={}, subject={}, bodyLength={}", to, subject, html == null ? 0 : html.length());
    }
}
