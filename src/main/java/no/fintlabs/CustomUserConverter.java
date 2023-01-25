package no.fintlabs;

import no.vigoiks.resourceserver.security.FintJwtDefaultConverter;

public class CustomUserConverter extends FintJwtDefaultConverter {
    public CustomUserConverter() {
        this.addMapping("organizationid", "ORGID_");
        this.addMapping("organizationnumber", "ORGNR_");
        this.addMapping("employeeId", "EMPLOYEE_");
    }
}