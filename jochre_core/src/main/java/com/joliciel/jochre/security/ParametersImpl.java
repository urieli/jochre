package com.joliciel.jochre.security;

import java.util.Date;

import com.joliciel.jochre.EntityImpl;

public class ParametersImpl extends EntityImpl implements ParametersInternal {
	private SecurityServiceInternal securityServiceInternal;
	private Date lastFailedLoginAttempt;
	private int captachaIntervalSeconds;
	
	@Override
	public void saveInternal() {
		this.securityServiceInternal.saveParametersInternal(this);
	}

	@Override
	public Date getLastFailedLoginAttempt() {
		return lastFailedLoginAttempt;
	}

	@Override
	public void setLastFailedLoginAttempt(Date lastFailedLoginAttempt) {
		this.lastFailedLoginAttempt = lastFailedLoginAttempt;
	}

	@Override
	public void loginFailed() {
		this.setLastFailedLoginAttempt(new Date());
	}

	@Override
	public int getCaptachaIntervalSeconds() {
		return captachaIntervalSeconds;
	}

	@Override
	public void setCaptachaIntervalSeconds(int captachaIntervalSeconds) {
		this.captachaIntervalSeconds = captachaIntervalSeconds;
	}

	public SecurityServiceInternal getSecurityServiceInternal() {
		return securityServiceInternal;
	}

	public void setSecurityServiceInternal(
			SecurityServiceInternal securityServiceInternal) {
		this.securityServiceInternal = securityServiceInternal;
	}

}
