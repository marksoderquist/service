package com.parallelsymmetry.service;

import java.io.InputStream;
import java.io.OutputStream;

public interface Plug {

	public InputStream getInputStream();

	public OutputStream getOutputStream();

}
