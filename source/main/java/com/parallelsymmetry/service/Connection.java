package com.parallelsymmetry.service;

public interface Connection {

	public abstract String getName();

	public abstract void connect() throws Exception;

	public abstract void waitFor() throws InterruptedException;

	public abstract void disconnect() throws Exception;

}
