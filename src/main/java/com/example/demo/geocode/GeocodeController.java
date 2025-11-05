package com.example.demo.geocode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/geocode", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeocodeController {

    @Value("${kakao.rest.apiKey}")
    private String kakaoRestKey;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @GetMapping
    public ResponseEntity<?> geocode(@RequestParam("query") String query) {
        try {
            String q = query == null ? "" : query.trim();
            if (q.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            List<GeocodeResult> results = searchKeyword(q);
            if (results.isEmpty()) results = searchAddress(q);

            return ResponseEntity.ok(results);

        } catch (HttpStatusCodeException httpEx) {
            return ResponseEntity.status(httpEx.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(httpEx.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    "{\"error\":\"geocode_failed\",\"message\":\"" + e.getMessage() + "\"}"
            );
        }
    }

    /** ✅ 한글 쿼리 인코딩 제대로 처리 (build().encode(UTF_8)) */
    private List<GeocodeResult> searchKeyword(String query) throws Exception {
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", query)     // 아직 인코딩 안 함
                .queryParam("size", 10)
                .build()                         // build(false)와 동일
                .encode(StandardCharsets.UTF_8)  // 여기서 인코딩
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> resp = rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) return List.of();

        JsonNode root = om.readTree(resp.getBody());
        JsonNode docs = root.path("documents");
        List<GeocodeResult> list = new ArrayList<>();
        for (JsonNode d : docs) {
            double lng = safeParse(d.path("x").asText());
            double lat = safeParse(d.path("y").asText());
            String name = textOr(d.path("place_name").asText(), d.path("address_name").asText());
            String addr = firstNonEmpty(
                    d.path("road_address_name").asText(),
                    d.path("address_name").asText(),
                    name
            );
            if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                list.add(new GeocodeResult(name, lat, lng, addr));
            }
        }
        return list;
    }

    /** ✅ 주소 검색도 동일하게 인코딩 처리 */
    private List<GeocodeResult> searchAddress(String query) throws Exception {
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                .queryParam("query", query)
                .queryParam("size", 10)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> resp = rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) return List.of();

        JsonNode root = om.readTree(resp.getBody());
        JsonNode docs = root.path("documents");
        List<GeocodeResult> list = new ArrayList<>();
        for (JsonNode d : docs) {
            JsonNode road = d.path("road_address");
            JsonNode lot  = d.path("address");

            String name = firstNonEmpty(
                    d.path("address_name").asText(),
                    road.path("address_name").asText(),
                    lot.path("address_name").asText()
            );

            double lng = Double.NaN, lat = Double.NaN;
            if (!road.isMissingNode() && !road.isNull()) {
                lng = safeParse(road.path("x").asText());
                lat = safeParse(road.path("y").asText());
            }
            if ((Double.isNaN(lat) || Double.isNaN(lng)) && !lot.isMissingNode() && !lot.isNull()) {
                lng = safeParse(lot.path("x").asText());
                lat = safeParse(lot.path("y").asText());
            }
            if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                list.add(new GeocodeResult(name, lat, lng, name));
            }
        }
        return list;
    }

    private static double safeParse(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
    }
    private static String textOr(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b != null ? b : "");
    }
    private static String firstNonEmpty(String... arr) {
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return "";
    }
}