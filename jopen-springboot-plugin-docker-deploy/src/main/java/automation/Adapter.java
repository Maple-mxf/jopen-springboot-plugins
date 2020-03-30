package automation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.io.File;

@Getter
@Setter
@Deprecated
public class Adapter {
    @Id
    private String id;
    // public ip
    private String host;
    // ssh connect port
    private int connectPort;
    // auth info
    private String username;
    private String password;
    private File secret;
    // lock
    private Long version;
    // previous login time
    private Long preLoginTime;
    // running docker image number
    private Integer dockerImageNum;
    // login type
    private AuthType authType;
    // private key file
    private String prvKeyFile;

    public enum AuthType {
        PRIVATE_KEY,
        PASSWORD
    }

    public Adapter(String host,
                   String username, String password,
                   int connectPort, AuthType authType, String prvKeyFile) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.connectPort = connectPort;
        this.authType = authType;
        this.prvKeyFile = prvKeyFile;
    }
}
