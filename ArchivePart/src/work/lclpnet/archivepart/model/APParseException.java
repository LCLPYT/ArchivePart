package work.lclpnet.archivepart.model;

import java.io.IOException;

public class APParseException extends IOException{

	private static final long serialVersionUID = -6528714898652622759L;
	private String message;
	
	public APParseException() {}
	
	public APParseException(String message) {
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}

}
