package edu.escuelaing.arem.ASE.app;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Set;

public class MyWebServices {
    public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException {
        //FOR STUDENTS API
        HashMap<String, String> students = new HashMap<>();

        HttpServer.get("/arep", (req, res) -> {
            String resp = "Hello AREP.";
            return resp;
        });

        HttpServer.get("/ieti", (req, res) -> {
            String resp = "Hello IETI.";
            return resp;
        });

        HttpServer.post("/students" , (req, res) -> {
            String studentId = req.queryParams("id");
            String studentName = req.queryParams("name");
            students.put(studentId, studentName);
            return "";
        });

        HttpServer.get("/students", (req, res) -> {
            res.type("application/json");
            String result = "{";
            for (String id : students.keySet()) {
                String name = students.get(id);
                result += "\"" + id + "\"" + ":" + "\"" + name + "\",";
            }

            String finalResult = "{}";
            if (result != "{") {
                finalResult = result.substring(0, result.length() - 1);
                finalResult += "}";
            }

            return finalResult;
        });

        //Cambio exitoso de directorio
        //HttpServer.location("/changing-directory");

        //Cambio no exitoso de directorio
        //HttpServer.location("/changing");

        HttpServer.getInstance().runServer(args);
    }
}
