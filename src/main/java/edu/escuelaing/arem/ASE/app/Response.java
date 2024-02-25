package edu.escuelaing.arem.ASE.app;

public class Response {
    private String contentType;
    private String header;
    public Response() {
        contentType = "text/html";
    }

    public String header() {
        header = "HTTP/1.1 200 OK\r\n"
                + "Content-Type:" + contentType + "; charset=utf-8\r\n"
                + "\r\n";
        return header;
    }

    public void type(String contentType) {
        this.contentType = contentType;
    }
}
