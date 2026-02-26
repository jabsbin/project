package com.example.demo.route;

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
@RequestMapping(value = "/api/route", produces = MediaType.APPLICATION_JSON_VALUE)
public class RouteController {

    @Value("${kakao.rest.apiKey}")
    private String kakaoKey;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @GetMapping
    public ResponseEntity<?> route(
            @RequestParam("origin") String origin,           // "lat,lng" 또는 한글 주소
            @RequestParam("dest") String dest,               // "lat,lng" 또는 한글 주소
            @RequestParam(value = "via", required = false) String via,    // (단일 경유지, 선택)
            @RequestParam(value = "vias", required = false) String vias    // (우회용 다중 경유지 JSON, 선택)
    ) {
        try {
            // 1) 좌표/주소 처리 → Kakao가 요구하는 "x,y(=lng,lat)" 문자열로 변환
            String originXY = toXY(origin);
            String destXY   = toXY(dest);

            // 2) 경유지 구성: (a) 단일 via → 1개, (b) 다중 vias(JSON) → N개
            List<String> waypointList = new ArrayList<>();
            if (via != null && !via.isBlank()) {
                waypointList.add(latlngToXY(via)); // "lat,lng" → "lng,lat"
            }
            if (vias != null && !vias.isBlank()) {
                // URL 쿼리로 넘어온 JSON 문자열(vias)을 파싱
                // (프론트에서 encodeURIComponent로 보내도 Spring이 자동 디코딩해줍니다)
                JsonNode root = om.readTree(vias);
                if (root.isArray()) {
                    for (JsonNode n : root) {
                        double lat = n.path("lat").asDouble(Double.NaN);
                        double lng = n.path("lng").asDouble(Double.NaN);
                        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                            waypointList.add(lng + "," + lat); // Kakao: x,y
                        }
                    }
                }
            }

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://apis-navi.kakaomobility.com/v1/directions")
                    .queryParam("origin", originXY)
                    .queryParam("destination", destXY)
                    .queryParam("priority", "RECOMMEND");

            // 3) waypoints는 파이프(|) 구분자 필요 → Spring이 '|'를 허용하지 않으므로 "%7C"로 합침
            if (!waypointList.isEmpty()) {
                String waypointsValue = String.join("%7C", waypointList); // 안전하게 인코딩된 파이프
                builder.queryParam("waypoints", waypointsValue);
            }

            // build(true): 파라미터를 추가 인코딩하지 않음(우리가 %7C를 직접 넣었기 때문)
            URI uri = builder.build(true).toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response =
                    rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"route_failed\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /** "lat,lng" 또는 한글 주소 → "lng,lat" */
    private String toXY(String src) throws Exception {
        String trimmed = src.trim();
        // 숫자,숫자면 그대로 변환
        if (trimmed.matches("^[0-9.+-]+\\s*,\\s*[0-9.+-]+$")) {
            return latlngToXY(trimmed);
        }
        // 아니면 키워드 검색 1건 지오코드
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", trimmed)
                .queryParam("size", 1)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> resp = rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalArgumentException("지오코드 실패: " + resp.getStatusCode());
        }

        JsonNode root = om.readTree(resp.getBody());
        JsonNode doc0 = root.path("documents").isArray() && root.path("documents").size() > 0
                ? root.path("documents").get(0)
                : null;

        if (doc0 == null) {
            throw new IllegalArgumentException("주소를 좌표로 변환할 수 없습니다: " + src);
        }

        double lng = Double.parseDouble(doc0.path("x").asText());
        double lat = Double.parseDouble(doc0.path("y").asText());
        return lng + "," + lat; // Kakao: x,y
    }

    /** "lat,lng" → "lng,lat" */
    private String latlngToXY(String latlng) {
        String[] parts = latlng.split(",");
        if (parts.length != 2) throw new IllegalArgumentException("좌표 형식은 'lat,lng' 이어야 합니다.");
        double lat = Double.parseDouble(parts[0].trim());
        double lng = Double.parseDouble(parts[1].trim());
        return lng + "," + lat;
    }
}