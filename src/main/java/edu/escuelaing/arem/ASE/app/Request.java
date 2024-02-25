package edu.escuelaing.arem.ASE.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class Request {
    private String req;
    private HashMap<String, String> queryHash;
    public Request(String req) {
        this.req = req;
        queryHash = new HashMap<>();
        try {
            setQueryHash();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void setQueryHash() throws URISyntaxException {
        URI fileUri = new URI(req);
        String queries = fileUri.getQuery();
        if (queries != null) {
            String[] splitQuery = queries.split("&");
            for(String eachQuery : splitQuery) {
                String[] separedQuery = eachQuery.split("=");
                queryHash.put(separedQuery[0], separedQuery[1]);
            }
        }
    }

    public String queryParams (String queryVal) {
        return queryHash.get(queryVal);
    }
}
