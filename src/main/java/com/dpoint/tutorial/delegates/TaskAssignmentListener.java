package com.dpoint.tutorial.delegates;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.context.Context;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskAssignmentListener implements TaskListener {

    private static final String HOST = "smtp.gmail.com";
    private static final String USER = "report@grupopositiva.com";

    private static final String PWD = "exwgrstcfpayzqzc";
    private final static Logger LOGGER = Logger.getLogger(TaskAssignmentListener.class.getName());

    @Override
    public void notify(DelegateTask delegateTask) {

        String assignee = delegateTask.getAssignee();
        String taskId = delegateTask.getId();

        if (assignee != null) {
            IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
            User user = identityService.createUserQuery().userId(assignee).singleResult();

            if (user != null) {
                String recipient = user.getEmail();
                Properties props = new Properties();
                props.setProperty("mail.transport.protocol", "smtp");
                props.setProperty("mail.host", "smtp.gmail.com");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.port", "465");
                props.put("mail.debug", "true");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
                Session session = Session.getDefaultInstance(props,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(USER, PWD);
                            }
                        });


                try {
                    Transport transport = session.getTransport();
                    InternetAddress addressFrom = new InternetAddress(USER);

                    MimeMessage message = new MimeMessage(session);
                    message.setSender(addressFrom);
                    message.setSubject("Nova tarefa: " + delegateTask.getName());
                    message.setContent("Uma nova tarefa foi atribuida para você, acesse aqui: http://localhost:8080/camunda/app/tasklist/default/#/task=" + taskId, "text/plain");
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

                    transport.connect();
                    Transport.send(message);
                    transport.close();

                    LOGGER.info("Tarefa enviado para o email com sucesso,  '" + assignee + "' com endereco '" + recipient + "'.");

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Não foi possível enviar email", e);
                }
            } else {
                LOGGER.warning("Email não foi enviado " + assignee + "', usuário não tem email cadastrado.");
            }
        } else {
            LOGGER.warning("Email não enviado " + assignee + "', o usuário não está registrado no serviço de identidade.");
        }
    }
}
