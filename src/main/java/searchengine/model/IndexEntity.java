package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "indices")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    PageEntity page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    LemmaEntity lemma;

    @Column(columnDefinition = "float", name = "`rank`", nullable = false)
    double rank;
}
