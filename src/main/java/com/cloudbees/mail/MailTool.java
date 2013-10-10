package com.cloudbees.mail;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class MailTool {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Session mailSession;
    protected Transport mailTransport;
    protected InternetAddress mailFrom;
    protected InternetAddress mailBcc;
    boolean test = false;

    public MailTool() throws Exception {
        InputStream smtpPropertiesAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("smtp.properties");
        Preconditions.checkNotNull(smtpPropertiesAsStream, "File '/smtp.properties' NOT found in the classpath");

        final Properties smtpProperties = new Properties();
        smtpProperties.load(smtpPropertiesAsStream);

        mailSession = Session.getInstance(smtpProperties, null);
        mailTransport = mailSession.getTransport();
        if (smtpProperties.containsKey("mail.username")) {
            mailTransport.connect(smtpProperties.getProperty("mail.username"), smtpProperties.getProperty("mail.password"));
        } else {
            mailTransport.connect();
        }
        try {
            mailFrom = new InternetAddress(smtpProperties.getProperty("mail.from"));
        } catch (Exception e) {
            throw new MessagingException("Exception parsing 'mail.from' from 'smtp.properties'", e);
        }

        String bcc = smtpProperties.getProperty("mail.bcc");
        if (!Strings.isNullOrEmpty(bcc)) {
            try {
                mailBcc = new InternetAddress(bcc);
            } catch (Exception e) {
                logger.warn("invalid 'mail.bcc=" + bcc + "'", e);
            }
        }

    }

    public static void main(String[] args) throws Exception {

        MailTool mailTool = new MailTool();
        mailTool.sendEmails();

    }

    public void sendEmails() throws Exception {

        Splitter splitter = Splitter.on('\t')
                .trimResults()
                .omitEmptyStrings();


        URL databasesUrl = Thread.currentThread().getContextClassLoader().getResource("email-data.txt");
        Preconditions.checkNotNull(databasesUrl, "File 'databases.txt' NOT found in the classpath");

        Set<String> lines = Sets.newTreeSet(Resources.readLines(databasesUrl, Charsets.ISO_8859_1));
        for (String line : lines) {
            try {
                line = line.trim();
                if (line.startsWith("#")) {
                    // skip
                } else {

                    Iterator<String> split = splitter.split(line).iterator();
                    String email = split.next();
                    String account = split.next();
                    String databases = split.next();
                    Map<String, String> templatesParams = Maps.newHashMap();
                    templatesParams.put("email", email);
                    templatesParams.put("accounts", account);
                    templatesParams.put("databases", databases);

                    sendEmail(templatesParams, email);
                }
            } catch (Exception e) {
                logger.warn("Exception processing line '{}'", line, e);
            }
        }
    }

    public void sendEmail(Map<String, String> templatesParams, String toAddress) throws MessagingException {
        sendEmail(templatesParams, new ArrayList<BodyPart>(), toAddress);
    }

    public void sendEmail(Map<String, String> templatesParams, List<BodyPart> attachments, String toAddress) throws MessagingException {

        if (test) {
            logger.info("TEST Send email to {}", toAddress);
            return;
        }
        logger.info("Send email to {}", toAddress);

        MimeBodyPart htmlAndPlainTextAlternativeBody = new MimeBodyPart();

        // TEXT AND HTML MESSAGE (gmail requires plain text alternative,
        // otherwise, it displays the 1st plain text attachment in the preview)
        MimeMultipart cover = new MimeMultipart("alternative");
        htmlAndPlainTextAlternativeBody.setContent(cover);
        BodyPart textHtmlBodyPart = new MimeBodyPart();
        String textHtmlBody = FreemarkerUtils.generate(templatesParams, "/com/cloudbees/mail/email.body.html.ftl");
        textHtmlBodyPart.setContent(textHtmlBody, "text/html");
        cover.addBodyPart(textHtmlBodyPart);

        BodyPart textPlainBodyPart = new MimeBodyPart();
        cover.addBodyPart(textPlainBodyPart);
        String textPlainBody = FreemarkerUtils.generate(templatesParams, "/com/cloudbees/mail/email.body.txt.ftl");
        textPlainBodyPart.setContent(textPlainBody, "text/plain");

        MimeMultipart content = new MimeMultipart("related");
        content.addBodyPart(htmlAndPlainTextAlternativeBody);

        // ATTACHMENTS
        for (BodyPart bodyPart : attachments) {
            content.addBodyPart(bodyPart);
        }

        MimeMessage msg = new MimeMessage(mailSession);

        msg.setFrom(mailFrom);
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
        // msg.addRecipient(Message.RecipientType.CC, mailFrom);
        if (mailBcc != null) {
            msg.addRecipient(Message.RecipientType.BCC, mailBcc);
        }

        String subject = FreemarkerUtils.generate(templatesParams, "/com/cloudbees/mail/email.subject.ftl");
        msg.setSubject(subject);
        msg.setContent(content);

        mailTransport.sendMessage(msg, msg.getAllRecipients());
    }
}
