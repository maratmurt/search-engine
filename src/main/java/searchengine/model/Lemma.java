package searchengine.model;

import javax.persistence.*;

@Entity
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    SiteEntity site;

    @Column(columnDefinition = "varchar(255)", nullable = false)
    String lemma;

    @Column(nullable = false)
    int frequency;
}
