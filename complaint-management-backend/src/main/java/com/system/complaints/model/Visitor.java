package com.system.complaints.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "visitors")  // Explicitly specify the table name
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the visitor

    @Column(nullable = false)
    private String name; // Visitor's name

    @Column(nullable = true)
    private String city; // City where the visitor resides (optional)

    @OneToMany(mappedBy = "visitor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Location> locations; // List of locations associated with this visitor

    // Default constructor required by JPA
    public Visitor() {
    }

    // Parameterized constructor for convenience
    public Visitor(String name, String city) {
        this.name = name;
        this.city = city;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    // toString method for debugging and logging purposes
    @Override
    public String toString() {
        return "Visitor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
