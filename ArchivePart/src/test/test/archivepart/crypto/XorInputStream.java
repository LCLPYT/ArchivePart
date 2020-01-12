package test.test.archivepart.crypto;

import java.io.IOException;
import java.io.InputStream;

public class XorInputStream extends InputStream{

	private InputStream in;
	private char[] password;
	private int i = 0;
	
	public XorInputStream(InputStream in, String password) {
		if(password == null || password.length() <= 0) throw new IllegalArgumentException("Password must not be null or empty.");
		
		this.in = in;
		this.password = password.toCharArray();
	}
	
	@Override
	public int read() throws IOException {
		int read = in.read();
		if(read == -1) return -1;
		
		if(i >= password.length) i = 0;
		return read ^ password[i++];
	}

}
