package work.lclpnet.archivepart.crypto;

import java.io.IOException;
import java.io.OutputStream;

public class XorOutputStream extends OutputStream{

	private OutputStream out;
	private char[] password;
	private int i = 0;
	
	public XorOutputStream(OutputStream out, String password) {
		if(password == null || password.length() <= 0) throw new IllegalArgumentException("Password must not be null or empty.");
		
		this.out = out;
		this.password = password.toCharArray();
	}
	
	@Override
	public void write(int b) throws IOException {
		if(i >= password.length) i = 0;
		out.write(b ^ password[i++]);
	}

}
