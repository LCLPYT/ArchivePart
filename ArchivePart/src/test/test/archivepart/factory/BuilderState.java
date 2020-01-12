package test.test.archivepart.factory;

import java.io.OutputStream;

public class BuilderState {
	
	public OutputStream currentOut;
	public long currentLength;
	public int currentPart;
	public long maxPartSize;
	public String password;

	public BuilderState(OutputStream currentOut, long currentLength, int currentPart, long maxPartSize, String password) {
		this.currentOut = currentOut;
		this.currentLength = currentLength;
		this.currentPart = currentPart;
		this.maxPartSize = maxPartSize;
		this.password = password;
	}
	
}