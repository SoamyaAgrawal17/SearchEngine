package algorithm;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public  class Util {

    private Util() {

    }

    static HashMap<String, Double> sortByComparator(HashMap<String, Double> unsortMap, final boolean order) {

        List<Map.Entry<String, Double>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) -> {
            if (order) return o1.getValue().compareTo(o2.getValue());
            else return o2.getValue().compareTo(o1.getValue());
        });

        // Maintaining insertion order with the help of LinkedList
        HashMap<String, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void printMap(Map<String, Double> map) {
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            log.info("Key : " + entry.getKey() + " Value : " + entry.getValue());
        }
    }

}
