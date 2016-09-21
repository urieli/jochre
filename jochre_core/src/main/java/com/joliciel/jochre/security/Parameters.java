package com.joliciel.jochre.security;

import java.util.Date;

public class Parameters {
	private int id;
	private Date lastFailedLoginAttempt;
	private int captachaIntervalSeconds;

	public static Parameters loadParameters() {
		SecurityDao securityDao = SecurityDao.getInstance();
		return securityDao.loadParameters();
	}

	void save() {
		SecurityDao securityDao = SecurityDao.getInstance();
		securityDao.saveParametersInternal(this);
	}

	/**
	 * When was the last failed login attempt on the entire system.
	 */
	public Date getLastFailedLoginAttempt() {
		return lastFailedLoginAttempt;
	}

	void setLastFailedLoginAttempt(Date lastFailedLoginAttempt) {
		this.lastFailedLoginAttempt = lastFailedLoginAttempt;
	}

	void loginFailed() {
		this.setLastFailedLoginAttempt(new Date());
	}

	/**
	 * The number of seconds that have to pass since the last failed login on the
	 * entire system, otherwise we should show a captcha.
	 */
	public int getCaptachaIntervalSeconds() {
		return captachaIntervalSeconds;
	}

	void setCaptachaIntervalSeconds(int captachaIntervalSeconds) {
		this.captachaIntervalSeconds = captachaIntervalSeconds;
	}

	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

}
