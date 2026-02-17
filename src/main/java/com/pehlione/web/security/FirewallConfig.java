package com.pehlione.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
public class FirewallConfig {

	@Bean
	HttpFirewall httpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();
		firewall.setAllowBackSlash(false);
		firewall.setAllowSemicolon(false);
		firewall.setAllowUrlEncodedSlash(false);
		firewall.setAllowUrlEncodedDoubleSlash(false);
		firewall.setAllowUrlEncodedPercent(false);
		return firewall;
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
		return web -> web.httpFirewall(firewall);
	}
}
