package com.smartdining.utils;

import java.util.Properties;

import com.smartdining.entity.Employee;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Gửi email qua Google mail.
 * Cần mail.username (địa chỉ Gmail) và mail.password (App Password 16 ký tự) trong config.properties.
 * @version 1.1
 * @author Nguyễn Nghiệm
 */
public class XMail {
    static final String APP_URL = ConfigUtil.get("app.url");
    static final String USERNAME = ConfigUtil.get("mail.username");
    static final String PASSWORD = ConfigUtil.get("mail.password");

    public static boolean isConfigured() {
        return USERNAME != null && !USERNAME.isBlank() && PASSWORD != null && !PASSWORD.isBlank();
    }

    /** Gửi mã xác thực trên luồng nền để không làm chậm request (SMTP có thể mất vài giây). */
    public static void sendVerificationCodeAsync(Employee employee, String code, long validMinutes) {
        if (!isConfigured()) {
            // Môi trường dev chưa cấu hình Gmail: in mã ra log server để vẫn kiểm thử được.
            System.out.println("[XMail] Chua cau hinh mail.username/mail.password. DEV - ma xac thuc cua "
                    + employee.getUsername() + " la: " + code);
            return;
        }
        String subject = "SmartDining - Mã xác thực tài khoản";
        String loginUrl = (APP_URL == null ? "" : APP_URL) + SecurityUtil.loginPath(employee.getRole());
        String body = "<p>Xin chào <b>" + escape(employee.getFullName()) + "</b>,</p>"
                + "<p>Tài khoản <b>" + escape(employee.getUsername()) + "</b> của bạn đã được tạo trên SmartDining.</p>"
                + "<p>Mã xác thực của bạn là: <b style=\"font-size:22px;letter-spacing:4px\">" + code + "</b></p>"
                + "<p>Mã có hiệu lực trong " + validMinutes + " phút. Hãy đăng nhập tại "
                + "<a href=\"" + escape(loginUrl) + "\">" + escape(loginUrl) + "</a> và nhập mã này.</p>"
                + "<p>Nếu bạn không yêu cầu tài khoản này, hãy bỏ qua email.</p>";
        String to = employee.getEmail();
        Thread.ofVirtual().start(() -> {
            if (!send(to, subject, body)) {
                // Gui that bai (sai App Password, mat mang...): in ma ra log server de van kich hoat duoc khi dev.
                System.out.println("[XMail] DEV - ma xac thuc cua " + employee.getUsername() + " la: " + code
                        + " (gui mail that bai)");
            }
        });
    }

    static public boolean send(String to, String subject, String content) {
        var from = "SmartDining <" + USERNAME + ">";
        return send(from, to, subject, content);
    }

    static public boolean send(String from, String to, String subject, String content) {
        var props = new Properties();
        props.setProperty("mail.smtp.host", "smtp.gmail.com");
        props.setProperty("mail.smtp.port", "587");
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        var session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            var mail = new MimeMessage(session);
            mail.setFrom(new InternetAddress(from));
            mail.setRecipients(Message.RecipientType.TO, to);
            mail.setSubject(subject, "utf8");
            mail.setContent(content, "text/html; charset=utf8");
            mail.setReplyTo(mail.getFrom());

            Transport.send(mail);
            System.out.println("Mail was sent to " + to);
            return true;
        } catch (MessagingException e) {
            System.err.println("[XMail] Gui mail that bai toi " + to + ": " + e.getMessage());
            return false;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
