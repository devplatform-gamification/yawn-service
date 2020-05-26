package com.devplatform.yawnservice.api;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-03-18T19:01:01.992Z[GMT]")
public class NotFoundException extends ApiException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6275508990187553410L;
	private int code;
    public NotFoundException (int code, String msg) {
        super(code, msg);
        this.setCode(code);
    }
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
}
