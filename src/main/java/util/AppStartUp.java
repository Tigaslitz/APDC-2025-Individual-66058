package util;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import org.apache.commons.codec.digest.DigestUtils;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import resources.LoginResource;

import java.util.logging.Logger;

@WebListener
public class AppStartUp implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartUp.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.fine("Aplicação arrancou — a verificar utilizador ROOT...");

        String username = "root";
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);

        Entity user = datastore.get(userKey);
        if (user == null) {
            Entity root = Entity.newBuilder(userKey)
                    .set("user_email", "root@root.com")
                    .set("user_pwd", DigestUtils.sha512Hex("rootadmin2025!"))
                    .set("user_name", "Root Admin")
                    .set("user_phone", "+351000000000")
                    .set("user_privacy", "privado")
                    .set("user_role", "ADMIN")
                    .set("account_status", "ATIVADA")
                    .set("user_creation_time", Timestamp.now())
                    .build();

            datastore.put(root);
            LOG.fine("Utilizador ROOT criado com sucesso!");
        } else {
            LOG.fine("Utilizador ROOT já existe.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Nada a fazer no fim
    }
}

