package no.nav.aura.fasit.rest.spring.jackson;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class LowerCaseStrategy extends PropertyNamingStrategies.NamingBase {
    @Override
    public String translate(String input) {
        return input == null ? null : input.toLowerCase();
    }
}