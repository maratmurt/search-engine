package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Data
@Table(indexes = @Index(columnList = "path"))
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    SiteEntity site;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    String path;

    @Column(nullable = false)
    int code;

    @Column(columnDefinition = "mediumtext", nullable = false)
    String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return Objects.equals(site, page.site) && Objects.equals(path, page.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, path);
    }
}
