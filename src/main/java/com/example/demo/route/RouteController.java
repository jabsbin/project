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
            @RequestParam("origin") String origin,
            @RequestParam("dest") String dest,
            @RequestParam(value = "via", required = false) String via
    ) {
        try {
            // 1) 원점/도착점이 위도,경도 형태인지 확인 → 아니면 지오코드 호출
            String originXY = toXY(origin);
            String destXY   = toXY(dest);

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://apis-navi.kakaomobility.com/v1/directions")
                    .queryParam("origin", originXY)
                    .queryParam("destination", destXY)
                    .queryParam("priority", "RECOMMEND");

            if (via != null && !via.isBlank()) {
                builder.queryParam("waypoints", toXY(via));
            }

            // 여기서 true 넣으면 이미 인코딩된 한글도 안전하게 감
            URI uri = builder.build(true).toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = rest.exchange(uri, HttpMethod.GET, entity, String.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"route_failed\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 입력값이 "37.5665,126.9780" 같이 숫자면 그대로 변환,
     * 아니면(=한글 주소나 지명) 카카오 지오코드 한번 더 호출해서 첫번째 결과를 좌표로 씀.
     * 카카오 내비는 "lng,lat" 순서를 요구하니까 마지막에 순서 바꿈.
     */
    private String toXY(String src) throws Exception {
        String trimmed = src.trim();
        // 숫자,숫자 형태면 그대로
        if (trimmed.matches("^[0-9.+-]+\\s*,\\s*[0-9.+-]+$")) {
            String[] parts = trimmed.split(",");
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            return lng + "," + lat; // kakao: x,y = lng,lat
        }

        // 여기로 왔으면 한글 주소 → 지오코드
        // (너 geocode 컨트롤러랑 똑같이 카카오 로컬 검색 한 번 더 때리는 로직)
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

        return lng + "," + lat;
    }
}