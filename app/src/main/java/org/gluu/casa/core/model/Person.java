package org.gluu.casa.core.model;

import org.gluu.casa.misc.Utils;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;

import java.util.List;

@DataEntry
@ObjectClass(values = { "top", "gluuPerson" })
public class Person extends BasePerson {

    @AttributeName
    private String givenName;

    @AttributeName(name = "sn")
    private String surname;

    // The field used for optional attribute sn.
    @AttributeName(name = "oxEnrollmentCode")
    private String enrollmentCode;

    @AttributeName
    private List<String> memberOf;

    public String getGivenName() {
        return givenName;
    }

    public String getSurname() {
        return surname;
    }

    public String getEnrollmentCode() {
        return enrollmentCode;
    }

    public List<String> getMemberOf() {
        return Utils.nonNullList(memberOf);
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setEnrollmentCode(String enrollmentCode) {
        this.enrollmentCode = enrollmentCode;
    }

    public void setMemberOf(List<String> memberOf) {
        this.memberOf = memberOf;
    }

}
