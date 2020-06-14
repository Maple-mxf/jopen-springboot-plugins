package standard.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.annotation.Id;

/**
 * @author maxuefeng
 * @since 2020/5/26
 */
public abstract class Entity implements java.io.Serializable {

    @Id
    protected String id;

    protected Long createTime;

    protected Long updateTime;

    protected String name;

    protected String title;

    protected String showName;

    protected Boolean deleted;

    protected String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    @NonNull
    public abstract String getCollectionName();
}
