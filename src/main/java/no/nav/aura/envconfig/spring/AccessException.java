package no.nav.aura.envconfig.spring;

import org.springframework.security.access.AccessDeniedException;

@SuppressWarnings("serial")
public class AccessException extends AccessDeniedException {

	public AccessException(String message) {
		super(message);
	}

}
