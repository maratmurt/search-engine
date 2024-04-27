package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "site")
public class Site {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "status", columnDefinition = "enum('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", columnDefinition = "datetime", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "url", columnDefinition = "varchar(255)", nullable = false)
    private String url;

    @Column(name = "name", columnDefinition = "varchar(255)", nullable = false)
    private String name;
}
