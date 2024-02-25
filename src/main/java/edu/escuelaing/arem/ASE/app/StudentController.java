package edu.escuelaing.arem.ASE.app;

import java.net.URI;
import java.util.HashMap;

@Component
public class StudentController {
    private static HashMap<String, String> students = new HashMap<>();

    @GetMapping("/students")
    public static String students() {
        students.put("1","Santiago");
        students.put("2","Andres");
        students.put("3","Juan");

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
    }
}
