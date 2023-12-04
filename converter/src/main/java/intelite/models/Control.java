package intelite.models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "control")
public class Control implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String file;
    @Column
    private Long sizef;

    public Control() {
        super();
    }

    public Control(String file, Long size) {
        this.file = file;
        this.sizef = size;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Long getSize() {
        return sizef;
    }

    public void setSize(Long size) {
        this.sizef = size;
    }

    @Override
    public String toString() {
        return "Control{" + "id=" + id + ", file=" + file + ", size=" + sizef + '}';
    }

}
