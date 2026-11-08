package it.univaq.vetcare.model;

import java.util.Objects;


public class Owner {

    private Integer id;
    private String firstName;
    private String lastName;
    private String fiscalCode;
    private String phone;
    private String email;
    private PreferredChannel preferredChannel;

    public Owner() {
    }

    public Owner(Integer id, String firstName, String lastName, String fiscalCode,
                 String phone, String email, PreferredChannel preferredChannel) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fiscalCode = fiscalCode;
        this.phone = phone;
        this.email = email;
        this.preferredChannel = preferredChannel != null ? preferredChannel : PreferredChannel.TELEFONO;
    }

    public Owner(String firstName, String lastName, String fiscalCode,
                 String phone, String email, PreferredChannel preferredChannel) {
        this(null, firstName, lastName, fiscalCode, phone, email, preferredChannel);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public void setFiscalCode(String fiscalCode) {
        this.fiscalCode = fiscalCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public PreferredChannel getPreferredChannel() {
        return preferredChannel;
    }

    public void setPreferredChannel(PreferredChannel preferredChannel) {
        this.preferredChannel = preferredChannel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Owner)) return false;
        Owner owner = (Owner) o;
        return Objects.equals(fiscalCode, owner.fiscalCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fiscalCode);
    }

    @Override
    public String toString() {
        return getFullName() + " [" + fiscalCode + "] - Tel: " + phone + " (" + preferredChannel + ")";
    }
}
