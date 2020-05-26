package com.devplatform.yawnservice.api;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-03-18T19:01:01.992Z[GMT]")
public class ApiException extends Exception{
    /**
	 * 
	 */
	private static final long serialVersionUID = 7321064481734360356L;
	@SuppressWarnings("unused")
	private int code;
    public ApiException (int code, String msg) {
        super(msg);
        this.code = code;
    }
}
