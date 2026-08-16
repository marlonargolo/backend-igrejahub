package com.igrejahub.accounting.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "journal_entry_lines")
@Data
public class JournalEntryLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;
    private Long accountId;
    private Long debitCents;
    private Long creditCents;
    private String description;
}
