package com.parallelsymmetry.service;

import java.io.InputStream;
import java.io.OutputStream;

public class PlugAdapter implements Plug {

	private InputStream input;

	private OutputStream output;

	public PlugAdapter( InputStream input, OutputStream output ) {
		this.input = input;
		this.output = output;
	}

	public InputStream getInputStream() {
		return input;
	}

	public OutputStream getOutputStream() {
		return output;
	}

}
