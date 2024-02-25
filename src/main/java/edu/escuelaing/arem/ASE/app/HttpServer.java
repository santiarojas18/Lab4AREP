package edu.escuelaing.arem.ASE.app;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hello world!
 *
 */
public class HttpServer
{
    private static HttpServer _instance = new HttpServer();
    private static ConcurrentHashMap<String, StringBuffer> cache;
    private static Map<String,WebService> services = new HashMap<String, WebService>();
    private static Map<String, WebService> servicesPost = new HashMap<String, WebService>();
    private static String locationStaticFiles = "/public";
    private static Map<String, Method> servicesSpring = new HashMap<String, Method>();



    private HttpServer(){}

    public static HttpServer getInstance() {
        return _instance;
    }

    //as default will search at public
    public void runServer(String[] args) throws IOException, URISyntaxException, ClassNotFoundException {
        cache = new ConcurrentHashMap<>();
        Class helloController = Class.forName(args[0]);
        if (helloController.isAnnotationPresent(Component.class)) {
            lookingForAnnotation(helloController);
        }

        ArrayList<Class<?>> classesToAddMethods = loadingClasses("edu", Component.class);
        System.out.println(classesToAddMethods);


        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }

        boolean running = true;
        while (running) {
            Socket clientSocket = null;
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            String inputLine;
            String outputLine = "";

            boolean firstLine = true;
            String uriStr = "";
            int counterLine = 1;

            //TO KNOW WHICH KIND OF HTTP REQUEST IS RECEIVING
            String requestType = null;

            while ((inputLine = in.readLine()) != null) {
                if(firstLine){
                    uriStr = inputLine.split(" ")[1];
                    firstLine = false;
                }

                if (counterLine == 1) {
                    requestType = inputLine.split(" ")[0];
                }

                System.out.println("Received: " + inputLine);
                counterLine++;
                if (!in.ready()) {
                    break;
                }
            }

            URI fileUri = new URI(uriStr);

            //EXTERNAL API CONNECTION
            HttpConnectionExample connectionToApi = new HttpConnectionExample();
            System.out.println(fileUri.getPath());

            String path = fileUri.getPath();
            if (uriStr.startsWith("/movies/")) {
                String[] pathAPI = fileUri.getPath().split("/");
                String movieTitle = pathAPI[2];
                outputLine = apiSearcher(connectionToApi, movieTitle);
            } else if (path.startsWith("/action")){
                String webUri = path.replace("/action", "");
                if (requestType.equals("GET")) {
                    outputLine = getTheAction(uriStr, webUri, services);
                } else if (requestType.equals("POST")) {
                    outputLine = getTheAction(uriStr, webUri, servicesPost);
                }
            } else if (!uriStr.startsWith("/favicon") && !servicesSpring.containsKey(uriStr)){
                outputLine = httpFileSearcher(fileUri.getPath(), clientSocket);
            } else if (servicesSpring.containsKey(uriStr)) {
                String header = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type:text/html; charset=utf-8\r\n"
                        + "\r\n";
                Method methodToInvoke = servicesSpring.get(uriStr);
                try {
                    outputLine = header + (String) methodToInvoke.invoke(null);
                } catch (Exception e) {
                }
            } else {
                outputLine = "";
            }
            out.println(outputLine);

            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    public static ArrayList<Class<?>> loadingClasses (String rootPacket, Class<?> annotation) {
        ArrayList<Class<?>> foundClasses = new ArrayList<>();

        try {
            String packetRoute = rootPacket.replace('.', '/');
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packetRoute);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                System.out.println("helloooooo");
                System.out.println(directory.getName());
                foundClasses.addAll(findingClassInDirectory(rootPacket, directory, annotation));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return foundClasses;
    }

    private static ArrayList<Class<?>> findingClassInDirectory(String packet, File directory, Class<?> annotation) {
        ArrayList<Class<?>> foundClasses = new ArrayList<>();

        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    foundClasses.addAll(findingClassInDirectory(
                            packet + "." + file.getName(), file, annotation));
                } else if (file.getName().endsWith(".class")) {
                    String className = packet + '.' + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> classToAdd = Class.forName(className);
                        if (classToAdd.isAnnotationPresent((Class<? extends Annotation>) annotation)) {
                            foundClasses.add(classToAdd);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return foundClasses;
    }

    private void lookingForAnnotation (Class helloController) {
        for (Method method : helloController.getMethods()) {
            if(method.isAnnotationPresent(GetMapping.class)) {
                String ruta = method.getAnnotation(GetMapping.class).value();
                Method m = method;
                servicesSpring.put(ruta, m);
            }
        }
    }

    private static String httpError() throws IOException {
        Path file = Paths.get("target/classes/public/error.html");
        String contentType = Files.probeContentType(file);

        String outputLine = "HTTP/1.1 404 NOT FOUND\r\n"
                + "Content-Type:" + contentType + ";charset=utf-8\r\n"
                + "\r\n";

        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                outputLine = outputLine + line;
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }

        return outputLine;

    }

    public static String httpFileSearcher(String fileName, Socket clientSocket) throws IOException {
        Path file = Paths.get("target/classes" + locationStaticFiles + fileName);
        String contentType = Files.probeContentType(file);
        //System.out.println("EL CONTENT TYPE: " + contentType);
        String outputLine = "HTTP/1.1 200 OK\r\n"
                + "Content-Type:" + contentType + ";charset=utf-8\r\n"
                + "\r\n";

        if (contentType != null && contentType.contains("image")) {
            imagesReader(file, clientSocket.getOutputStream());
            outputLine = "";
        } else {
            Charset charset = Charset.forName("UTF-8");
            try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    outputLine = outputLine + line;
                }
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
                outputLine = httpError();
            }
        }
        return outputLine;
    }

    public static void imagesReader(Path file, OutputStream outputStream) throws IOException {
        String contentType = Files.probeContentType(file);
        try (InputStream inputStream = Files.newInputStream(file)) {
            String header = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type:" + contentType + "\r\n"
                    + "Content-Length: " + Files.size(file) + "\r\n"
                    + "\r\n";
            outputStream.write(header.getBytes());

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        outputStream.close();
    }

    private static String apiSearcher(HttpConnectionExample connectionToApi, String movieTitle) throws IOException {
        StringBuffer apiResponse = new StringBuffer();
        String outputLine;
        //Verifies if the movie title has not been requested
        if (!cache.containsKey(movieTitle)) {
            //External API request
            try {
                apiResponse = connectionToApi.getMovieInfo(movieTitle);
                cache.put(movieTitle, apiResponse);
            } catch (IOException e) {
                System.err.format("IOException: %s%n", e);
                outputLine = httpError();
            }
        } else {
            apiResponse = cache.get(movieTitle);
        }

        outputLine = "HTTP/1.1 200 OK\r\n"
                + "Content-Type:application/json; charset=utf-8\r\n"
                + "\r\n"
                + apiResponse.toString();
        return outputLine;
    }

    private String getTheAction(String uriStr, String webUri, Map<String, WebService> hashOfServices) {
        String outputLine = "";
        if (hashOfServices.containsKey(webUri)) {
            Request req = new Request(uriStr);
            Response res = new Response();
            String temporalRes = hashOfServices.get(webUri).handle(req, res);
            outputLine = res.header() + temporalRes;
        }
        return outputLine;
    }

    public static void get(String r, WebService s) {
        services.put(r, s);
    }

    public static void post(String r, WebService s) {
        servicesPost.put(r, s);
    }

    public static void location(String newStartFiles) {
        locationStaticFiles = newStartFiles;
    }
}
