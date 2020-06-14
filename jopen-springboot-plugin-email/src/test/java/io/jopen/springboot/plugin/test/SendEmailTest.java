package io.jopen.springboot.plugin.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author maxuefeng
 * @since 2020/2/4
 */

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootApplication
@EnableAutoConfiguration
public class SendEmailTest {

    @Autowired
    private JavaMailSender javaMailSender;

    @Test
    @PostMapping
    public void testSend(HttpServletRequest request) {


        String method = request.getMethod();

        send();
    }

    private void send() {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom("kaiyuanshishen@163.com");
        mailMessage.setTo("2446011668@qq.com");
        mailMessage.setSubject("测试邮件发送");
        mailMessage.setText("测试text");

        javaMailSender.send(mailMessage);
    }
}
