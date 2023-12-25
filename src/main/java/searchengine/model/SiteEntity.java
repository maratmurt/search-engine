package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "datetime", nullable = false)
    private LocalDateTime statusTime;

    @Column(columnDefinition = "text")
    private String lastError;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    private String name;
}
