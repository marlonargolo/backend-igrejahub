package com.igrejahub.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Data
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long organizationId;
    private Long churchId;
    private String entryNumber;
    private LocalDate entryDate;
    private String description;
    private String status;
    private String reference;
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL)
    private List<JournalEntryLine> lines = new ArrayList<>();
}
