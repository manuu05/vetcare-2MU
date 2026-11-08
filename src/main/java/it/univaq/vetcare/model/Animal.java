package it.univaq.vetcare.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Animal {

    private Integer id;
    private String medicalRecordCode;
    private String name;
    private String species;
    private String breed;
    private LocalDate birthDate;
    private String sex;
    private String microchip;
    private double weight;
    private String notes;

    private List<Owner> owners = new ArrayList<>();
    private List<Allergy> allergies = new ArrayList<>();

    public Animal() {
    }

    public Animal(Integer id, String medicalRecordCode, String name, String species,
                  String breed, LocalDate birthDate, String sex, String microchip,
                  double weight, String notes) {
        this.id = id;
        this.medicalRecordCode = medicalRecordCode;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.sex = sex;
        this.microchip = microchip;
        this.weight = weight;
        this.notes = notes;
    }

    public Animal(String name, String species, String breed, LocalDate birthDate,
                  String sex, String microchip, double weight, String notes) {
        this(null, null, name, species, breed, birthDate, sex, microchip, weight, notes);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMedicalRecordCode() {
        return medicalRecordCode;
    }

    public void setMedicalRecordCode(String medicalRecordCode) {
        this.medicalRecordCode = medicalRecordCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getMicrochip() {
        return microchip;
    }

    public void setMicrochip(String microchip) {
        this.microchip = microchip;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Owner> getOwners() {
        return owners;
    }

    public void setOwners(List<Owner> owners) {
        this.owners = owners;
    }

    public List<Allergy> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<Allergy> allergies) {
        this.allergies = allergies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Animal)) return false;
        Animal animal = (Animal) o;
        return Objects.equals(microchip, animal.microchip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(microchip);
    }

    @Override
    public String toString() {
        return name + " (" + species + " - Microchip: " + microchip + ")";
    }
}
