package unitard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class Main {

    @SuppressWarnings("unchecked")
    public static Stuff copyJsonNode(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> map = mapper.convertValue(node, Map.class);
        return Stuff.fromMap(map);
    }

    public static final JsonNode jsonNode;

    static {
        JsonNode n;
        try {
            n = new ObjectMapper().readTree(new File("buy_response.json"));
        } catch (IOException e) {
            n = null;
        }
        jsonNode = n;
    }

    public static final  Stuff stuff = copyJsonNode(jsonNode);

    public static void main(String[] args) throws IOException {
        Map<Object,Object> map = new HashMap<>();
        Map<Object,Object> map2 = new HashMap<>();
        map.put("a", map2);
        map2.put("aa", Arrays.asList("aardvark", "apple", "agriculture"));


        //Hopefully<Map<String,Object>> r = stuff.get().asMapOf(String.class, Object.class);
        Hopefully<String> r1 = stuff.get("tieredResults", 0, "results", 0, "channel").as(String.class);
        Hopefully<String> r2 = stuff.get("tieredResults", 0, "results", 7, "channel").as(String.class);
        Hopefully<String> r3 = stuff.get("tieredResults", 0, "results", "?", "channel").as(String.class);
        Hopefully<Boolean> r4 = stuff.get("tieredResults", 0, "results", 0, "channel").as(Boolean.class);
        Hopefully<String> r5 = stuff.get("tieredResults").asListOf(Object.class).map(o -> o.toString().substring(0, 20));

        System.out.println(r1);
        System.out.println(r2);
        System.out.println(r3);
        System.out.println(r4);
        System.out.println(r5);

        stuff.at("tieredResults", 0, "results", 0).remove("channel").done();



        System.out.println("---------- REMOVE TEST ------------");

        Stuff listed = Stuff.listOf("a", "b", "c", 1, 2, 3);
        Stuff mapped = Stuff.mapOf("a", 1, "b", 2, "c", 3, "X");
        System.out.println("Listed: " + listed);
        System.out.println("Mapped: " + mapped);

        Stuff fromMap = Stuff.fromMap(map);

        System.out.println(fromMap);
        System.out.println(fromMap.remove("a"));
        System.out.println(fromMap.at("a").remove("aa").done());

        System.out.println("---------- ADD TEST ------------");

        System.out.println(fromMap);
        System.out.println(fromMap.at("a").put("b", "BOB"));
        System.out.println(fromMap.at("a").put("b", "BOB").done().get("a", "b"));
        System.out.println(
                fromMap.at("a").put("b", Stuff.EMPTY_MAP).at("b").put("c", "BOB").done().get("a", "b", "c"));
    }

}

