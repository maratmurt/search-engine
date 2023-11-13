package searchengine.model;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;

    @Column(columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    Status status;

    @Column(columnDefinition = "datetime", nullable = false)
    LocalDateTime statusTime;

    @Column(columnDefinition = "text")
    String lastError;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    String url;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteEntity site = (SiteEntity) o;
        return Objects.equals(url, site.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
