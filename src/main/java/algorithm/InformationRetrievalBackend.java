package algorithm;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.stemmer.PorterStemmer;
import org.apache.commons.codec.language.Metaphone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class InformationRetrievalBackend {

    private String fileName = "/corpus/datasets.json";

    private TreeMap<String, Integer> initialProcessing(String query) {
        TreeMap<String, Integer> queryMap = new TreeMap<>();
        //Query tokenization
        PTBTokenizer<CoreLabel> ptbtQuery = new PTBTokenizer<>(new StringReader(query), new CoreLabelTokenFactory(), "");

        while (ptbtQuery.hasNext()) {
            CoreLabel queryToken = ptbtQuery.next();
            // Query Stemming begins
            PorterStemmer s = new PorterStemmer();
            String querystring = queryToken.toString();
            querystring = querystring.toLowerCase();
            for (int c = 0; c < querystring.length(); c++) {
                s.add(querystring.charAt(c));
            }
            s.stem();
            String queryTerm;
            queryTerm = s.toString();

            if (queryTerm.matches("[a-zA-Z][a-z]+")) {

                // Query Metaphone begins
                Metaphone metaphone = new Metaphone();
                queryTerm = metaphone.encode(queryTerm);
            }
            Integer freq = queryMap.get(queryTerm);
            queryMap.put(queryTerm, (freq == null) ? 1 : freq + 1);
        }
        return queryMap;
    }


    ArrayList<String> searchResult(String query) throws IOException {
        TreeMap<String, Integer> queryMap = initialProcessing(query);

        // Corpus-retrieving of documents from json file
        String json;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fileName), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            json = sb.toString();
        }
        JSONArray jsonArray = new JSONArray(json);

        // 'finalTermFrequencyMap' is the TreeMap that displays the final document with dictionary
        // terms as tokens and integer value as document frequency
        TreeMap<String, Integer> finalTermFrequencyMap = new TreeMap<>();

        // Making an array list of all the individual Treemaps that represent
        // individual documents (in terms of tokens and term frequency).
        ArrayList<TreeMap<String, Integer>> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            String requestText = object.getString("request_text");

            //Document Tokenization begins
            TreeMap<String, Integer> individualTermFrequency = initialProcessing(requestText);
            for (Map.Entry<String, Integer> entry : individualTermFrequency.entrySet()) {
                String key = entry.getKey();
                Integer freq = finalTermFrequencyMap.get(key);
                finalTermFrequencyMap.put(key, (freq == null) ? 1 : freq + 1);
            }

            list.add(individualTermFrequency);
        }
        //Total Number of Documents-'totalDocuments'
        int totalDocuments = list.size();
        TreeMap<String, Double> rankedProduct = new TreeMap<>();

        for (Map.Entry<String, Integer> entry : finalTermFrequencyMap.entrySet()) {

            String key = entry.getKey();
            Integer documentFrequency = entry.getValue();
            Double rankedValue = (totalDocuments - documentFrequency + 0.5) / (documentFrequency + 0.5);
            rankedProduct.put(key, rankedValue);
        }

        // Making a HashMap that contains dictionary tokens and their final
        // product value which would be used to keep ranking of documents
        HashMap<String, Double> unsortMap = new HashMap<>();
        int i = 1;
        for (TreeMap<String, Integer> d : list) {
            Double product = 1.00;
            for (Map.Entry<String, Integer> entry : queryMap.entrySet()) {

                String key = entry.getKey();
                if (d.containsKey(key)) {
                    product = product * (rankedProduct.get(key));

                }
            }
            unsortMap.put("Doc " + i, product);
            i++;
        }
        // Making a new HashMap that would sort the HashMap that contained key
        // and unsorted product ranks in descending order
        HashMap<String, Double> sortedMapDesc = Util.sortByComparator(unsortMap, false);
        ArrayList<String> sortedOutput = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedMapDesc.entrySet()) {

            String key = entry.getKey();
            Double d = entry.getValue();
            sortedOutput.add(key);
            log.info(key + "   " + d);
        }

        return sortedOutput;
    }

    String getDocumentData(String data) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(fileName), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            line = br.readLine();
        }
        br.close();
        String content = null;
        try {
            JSONArray array = new JSONArray(sb.toString());
            JSONObject object;

            if (data != null) {
                object = array.getJSONObject(Integer.parseInt(data.substring(4)));
                content = object.getString("request_text");
            }

        } catch (JSONException ex) {
            log.error(ex.getMessage());
        }
        return content;
    }

}
