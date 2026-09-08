package com.example.carnet_contact_backend.repository;

import com.example.carnet_contact_backend.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {
}
