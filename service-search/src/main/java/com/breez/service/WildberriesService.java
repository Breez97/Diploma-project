package com.breez.service;

import com.breez.exception.ClientException;
import com.breez.exception.ServerException;
import com.breez.util.wildberries.WildberriesAllProductsUtil;
import com.breez.util.wildberries.WildberriesSingleProductUtil;
import com.breez.util.wildberries.WildberriesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.breez.constants.Constants.*;

@Service
public class WildberriesService implements HttpService {

	private final HttpClient httpClient;
	private final WildberriesUtil wildberriesUtil;
	private final WildberriesSingleProductUtil wildberriesSingleProductUtil;
	private final WildberriesAllProductsUtil wildberriesAllProductsUtil;

	@Autowired
	public WildberriesService(HttpClient httpClient,
							  @Qualifier("wildberriesUtil") WildberriesUtil wildberriesUtil,
							  @Qualifier("wildberriesSingleProductUtil") WildberriesSingleProductUtil wildberriesSingleProductUtil,
							  @Qualifier("wildberriesAllProductsUtil") WildberriesAllProductsUtil wildberriesAllProductsUtil) {
		this.httpClient = httpClient;
		this.wildberriesUtil = wildberriesUtil;
		this.wildberriesSingleProductUtil = wildberriesSingleProductUtil;
		this.wildberriesAllProductsUtil = wildberriesAllProductsUtil;
	}

	@Override
	public List<Map<String, Object>> makeRequest(Map<String, String> parameters) throws IOException, InterruptedException {
		String url = WILDBERRIES_BASE_URL + "dest=-1257786&hide_dtype=13&lang=ru&page=" + parameters.get("page") +
				"&query=" + parameters.get("title") + "&resultset=catalog&sort=" + parameters.get("sort") +
				"&spp=30&suppressSpellcheck=false";
		String responseBody = getResponseBody(url);
		return wildberriesAllProductsUtil.getAllProductsFromResponse(responseBody);
	}

	@Override
	public Map<String, Object> makeRequestProduct(long id) throws IOException, InterruptedException {
		String url = getProductInfoLink(id);
		String responseBody = getResponseBody(url);
		Map<String, Object> data = wildberriesAllProductsUtil.getProductInfoFromResponse(id, responseBody);
		String descriptionAndOptionsUrl = getDescriptionAndOptionsInfoLink(id);
		String responseBodyDescriptionAndOptions = getResponseBody(descriptionAndOptionsUrl);
		return wildberriesSingleProductUtil.getDescriptionAndOptionsFromResponse(responseBodyDescriptionAndOptions, data);
	}

	@Override
	public Map<String, String> getSearchParameters(String title, String sort, String page) {
		Map<String, String> parameters = new HashMap<>();
		String resultTitle = title.replace(" ", "+");
		String resultSort = switch (sort) {
			case COMMON_SORT_NEW -> WILDBERRIES_SORT_NEW;
			case COMMON_SORT_PRICE_ASC -> WILDBERRIES_SORT_PRICE_ASC;
			case COMMON_SORT_PRICE_DESC -> WILDBERRIES_SORT_PRICE_DESC;
			case COMMON_SORT_RATING -> WILDBERRIES_SORT_RATING;
			default -> WILDBERRIES_SORT_POPULAR;
		};
		parameters.put("title", resultTitle);
		parameters.put("sort", resultSort);
		parameters.put("page", page);
		return parameters;
	}

	private String getResponseBody(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("accept", "*/*")
				.header("accept-language", "en,en-US;q=0.9,ru;q=0.8")
				.header("origin", "https://www.wildberries.ru")
				.header("priority", "u=1, i")
				.header("sec-ch-ua", "Google Chrome;v=135, Not-A.Brand;v=8, Chromium;v=135")
				.header("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
				.header("x-captcha-id", WILDBERRIES_X_CAPTCHA_ID)
				.header("x-queryid", WILDBERRIES_X_QUERY_ID)
				.GET()
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		int statusCode = response.statusCode();
		if (statusCode == 200) {
			return response.body();
		} else if (statusCode >= 400 && statusCode <= 499) {
			throw new ClientException(HttpStatus.valueOf(statusCode), "Wildberries: Client error");
		} else if (statusCode >= 500 && statusCode <= 599) {
			throw new ServerException(HttpStatus.valueOf(statusCode), "Wildberries: Server error");
		}
		return null;
	}

	private String getProductInfoLink(long id) {
		return WILDBERRIES_PRODUCT_INFO_LINK + id;
	}

	public String getDescriptionAndOptionsInfoLink(long id) {
		long vol = id / 100000;
		long part = id / 1000;
		String basketNum = wildberriesUtil.getBasketNum(vol);

		return String.format("https://basket-%s.wbbasket.ru/vol%d/part%d/%d/info/ru/card.json", basketNum, vol, part, id);
	}

}
