package automation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.HashIndexed;

@Getter
@Setter
//@Document(collection = "deploy_task_log")
@Deprecated
public class TaskLog implements java.io.Serializable {

    @Id
    private String id;

    // @see Member#getId()
    @HashIndexed
    private String operatorMember;

    // @see hardware#getId()
    private String hardwareId;

    // @see adapter#getId()
    private String adapterId;

    // @see docker#getId()
    private String dockerId;

    private Boolean success;

    private Long time;
}
