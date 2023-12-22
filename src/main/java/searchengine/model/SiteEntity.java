package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

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
}
