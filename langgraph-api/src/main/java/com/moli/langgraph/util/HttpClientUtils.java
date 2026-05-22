package com.moli.langgraph.util;


import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class HttpClientUtils {


    private static CloseableHttpClient closeableHttpClient;

    static {
        PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager();
        //最大连接数
        clientConnectionManager.setMaxTotal(400);
        //每个路由【route指的是域名】的最大连接数
        clientConnectionManager.setDefaultMaxPerRoute(200);
        //校验链接是否有效
        clientConnectionManager.setValidateAfterInactivity(5000);

        closeableHttpClient = HttpClients
                .custom()
                .setConnectionManager(clientConnectionManager)
                .setDefaultRequestConfig(defaultRequestConfig())
                .build();

    }

    private static RequestConfig defaultRequestConfig() {
        return RequestConfig.custom()
                //建立tcp连接的超时
                .setConnectTimeout(3000)
                //指从连接池获取连接的timeout
                .setConnectionRequestTimeout(1000)
                //读取数据的timeout
                .setSocketTimeout(5 * 60 * 1000)
                .build();
    }

    /**
     * 发送Get请求
     *
     * @param url            请求地址 带参数
     * @param requestHeaders 请求头
     * @return 返回结果
     */
    public static Map<String, Object> getWithCookie(String url, Map<String, String> requestHeaders) {
        long start = System.currentTimeMillis();
        log.info("###模拟http请求发起：url:{},header:{}", url, JsonUtil.obj2StringWithoutSensitiveField(requestHeaders));
        Map<String, Object> resultMap = Maps.newHashMap();

        BasicCookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig())
                .setDefaultCookieStore(cookieStore).build();

        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse httpResponse = null;
        int statusCode = 200;
        String result = "";
        //添加请求头
        if (requestHeaders != null) {
            for (String key : requestHeaders.keySet()) {
                httpGet.addHeader(key, requestHeaders.get(key));
            }
        }
        try {
            httpResponse = httpClient.execute(httpGet);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            result = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            List<Cookie> cookies = cookieStore.getCookies();
            resultMap.put("resultJson", result);
            resultMap.put("cookies", cookies);
            long end = System.currentTimeMillis();
            long cost = (end - start);
            parseStatusCode(statusCode, result, cost);
        } catch (ConnectTimeoutException e) {
            log.error("###请求链接超时,url:{} {}", statusCode, url, e);
            throw new RuntimeException("系统繁忙，请稍后再尝试一下吧！");
        } catch (SocketTimeoutException e) {
            log.error("###超时,url:{} {}", statusCode, url, e);
            throw new RuntimeException("系统繁忙，请稍后再尝试一下吧！");
        } catch (IOException | ParseException e) {
            log.error("", e);
        } finally {
            close(httpClient, httpResponse);
        }
        long end = System.currentTimeMillis();
        long cost = (end - start);
        if (StringUtils.isNotBlank(result)) {
            String resLog = result;
            log.info("###模拟http请求结束，耗时{}毫秒，响应：{} {}", cost, statusCode, resLog.length() < 4096 ? resLog : resLog.substring(0, 4096));
        } else {
            log.info("###模拟http请求结束，耗时{}毫秒，响应：{} 无", cost, statusCode);
        }
        return resultMap;
    }


    /**
     * 发送Get请求
     *
     * @param url 请求地址
     * @return
     */
    public static String get(String url) {
        return get(url, null);
    }

    public static String get(String url, Map<String, Object> params) {
        return get(url, params, null);
    }

    /**
     * GET 请求 正常返回第三方接口文件输入流的字节数组
     *
     * @param url
     * @param params
     * @param headers
     * @return
     */
    public static byte[] downloadByteArray(String url, Map<String, Object> params, Map<String, String> headers) {
        long start = System.currentTimeMillis();
        HttpGet httpRequestBase = getHttpGet(url, params, headers);
        CloseableHttpResponse httpResponse = null;
        InputStream inputStream = null;
        int statusCode = 200;
        URI uri = null;
        try {
            uri = httpRequestBase.getURI();
            httpResponse = closeableHttpClient.execute(httpRequestBase);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            long end = System.currentTimeMillis();
            long cost = (end - start);
            if (statusCode == 503) {
                log.error("###模拟http请求结束，状态码：{}，耗时{}毫秒", statusCode, cost);
                throw new RuntimeException("服务不可用，请稍后再尝试一下吧");
            }
            if (statusCode == 504) {
                log.error("###模拟http请求结束，状态码：{}，耗时{}毫秒", statusCode, cost);
                throw new RuntimeException("请求超时，请稍后再尝试一下吧");
            }
            Header[] headerArr = httpResponse.getHeaders(HttpHeaders.CONTENT_DISPOSITION);
            // 无文件可下载
            if (Objects.isNull(headers) || headerArr.length <= 0) {
                return null;
            }
            inputStream = httpResponse.getEntity().getContent();
            return IOUtils.toByteArray(inputStream);
        } catch (ConnectTimeoutException e) {
            log.error("###请求服务器连接超时,url:{} {}", statusCode, uri, e);
            throw new RuntimeException("系统繁忙，请稍后再尝试一下吧！");
        } catch (SocketTimeoutException e) {
            log.error("###请求服务器响应超时,url:{} {}", statusCode, uri, e);
            throw new RuntimeException("系统繁忙，请稍后再尝试一下吧！");
        } catch (IOException | ParseException e) {
            log.error("", e);
        } finally {
            close(null, httpResponse);
            IOUtils.closeQuietly(inputStream);
        }
        long end = System.currentTimeMillis();
        long cost = (end - start);
        log.info("###模拟http请求结束，耗时{}毫秒，响应：{} 无", cost, statusCode);
        return null;
    }

    /**
     * 发送Get请求
     *
     * @param url     请求地址 带参数
     * @param params  查询参数
     * @param headers 请求头
     * @return 返回结果
     */
    public static String get(String url, Map<String, Object> params, Map<String, String> headers) {
        HttpGet httpGet = getHttpGet(url, params, headers);
        return getString(httpGet);
    }

    /**
     * 代码抽取 - 获取GET请求中所需要的HttpGet实类
     *
     * @param url
     * @param params
     * @param headers
     * @return
     */
    private static HttpGet getHttpGet(String url, Map<String, Object> params, Map<String, String> headers) {
        log.info("###模拟http请求发起：url:{},header:{},params:{}", url, JsonUtil.obj2StringWithoutSensitiveField(headers),
                JsonUtil.obj2StringWithoutSensitiveField(params));
        HttpGet httpGet = new HttpGet(url);
        //添加请求头
        if (headers != null) {
            for (String key : headers.keySet()) {
                httpGet.addHeader(key, headers.get(key));
            }
        }
        //添加参数
        if (params != null) {
            url = appendParam(url, params);
        }
        httpGet.setURI(URI.create(url));
        return httpGet;
    }

    public static String post(String url, String body) {
        return post(url, body, null);
    }

    public static String post(String url, String body, Map<String, String> headers) {
        return post(url, body, headers, null);
    }

    /**
     * 发送post请求
     *
     * @param url     请求地址
     * @param body    请求参数
     * @param headers 请求头
     * @return 第三方返回结果
     */
    public static String post(String url, String body, Map<String, String> headers, Map<String, Object> param) {
        log.info("###模拟http请求发起：url:{},header:{} param:{},body:{}", url,
                JsonUtil.obj2StringWithoutSensitiveField(headers), param, JsonUtil.obj2StringWithoutSensitiveField(body));
        HttpPost httpPost = new HttpPost();
        //添加请求头
        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.addHeader(key, headers.get(key));
            }
        }
        if (param != null) {
            url = appendParam(url, param);
        }
        httpPost.setURI(URI.create(url));
        if (body != null) {
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        }
        return getString(httpPost);
    }

    /**
     * 发送post请求
     *
     * @param url           请求地址
     * @param multipartFile 请求参数
     * @param headers       请求头
     * @param fileName      form-data中的文件名，如file
     * @param body          请求体参数
     * @param formParams    form-data表单的其它参数
     * @return 第三方返回结果
     */
    public static String postFile(String url, MultipartFile multipartFile, Map<String, Object> params, Map<String, String> headers, String fileName, String body, Map<String,
            String> formParams) {
        HttpPost httpPost = new HttpPost(url);
        //添加请求头
        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.addHeader(key, headers.get(key).toString());
            }
        }
        //添加参数
        if (params != null) {
            url = appendParam(url, params);
        }
        httpPost.setURI(URI.create(url));
        if (body != null) {
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
        }

        if (multipartFile != null) {
            try {
                //创建 MultipartEntityBuilder,以此来构建我们的参数
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
                //上传我们的文件
                entityBuilder.addBinaryBody(fileName, multipartFile.getInputStream(), ContentType.MULTIPART_FORM_DATA
                        , multipartFile.getOriginalFilename());
                // 追加form表单中的其它参数
                if (!CollectionUtils.isEmpty(formParams)) {
                    formParams.forEach(entityBuilder::addTextBody);
                }
                httpPost.setEntity(entityBuilder.build());
            } catch (Exception e) {
                throw new RuntimeException("附件上传失败");
            }
        }
        return getString(httpPost);
    }

    /**
     * 发送post请求
     *
     * @param url  请求地址
     * @param body 数据
     * @return 第三方返回结果
     */
    public static String put(String url, String body) {
        log.info("###模拟http请求发起：url:{},body:{}", url, body);
        HttpPut httpPut = new HttpPut(url);
        if (StringUtils.isNotBlank(body)) {
            StringEntity stringEntity = new StringEntity(body, Charset.forName("UTF-8"));
            httpPut.setEntity(stringEntity);
        }
        return getString(httpPut);
    }

    /**
     * 发送XML Post请求
     *
     * @param url     请求地址
     * @param xml     请求参数
     * @param headers 请求头
     * @return 返回结果
     */
    public static String postXml(String url, String xml, Map<String, String> headers) {
        log.info("###模拟http请求发起：url:{},xml:{},header:{}", url, xml, JsonUtil.obj2StringWithoutSensitiveField(headers));
        HttpPost httpPost = new HttpPost(url);
        //添加请求头
        httpPost.addHeader("Content-Type", "text/xml");
        if (headers != null) {
            for (String key : headers.keySet()) {
                httpPost.addHeader(key, headers.get(key));
            }
        }
        httpPost.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
        //执行
        return getString(httpPost);
    }


    /**
     * 发送put请求
     *
     * @param url            请求地址
     * @param body           请求json
     * @param requestHeaders 请求头
     * @return 返回结果
     */
    public static String put(String url, String body, Map<String, String> requestHeaders) {
        log.info("###模拟http请求发起：url:{},header:{},body:{}", url,
                JsonUtil.obj2StringWithoutSensitiveField(requestHeaders), body);
        HttpPut httpPut = new HttpPut(url);
        //添加请求头
        httpPut.addHeader("Content-Type", "application/json;charset=utf-8");
        if (requestHeaders != null) {
            for (String key : requestHeaders.keySet()) {
                httpPut.addHeader(key, requestHeaders.get(key));
            }
        }
        httpPut.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        //执行
        return getString(httpPut);
    }

    /**
     * 发送delete请求
     *
     * @param url            请求地址
     * @param requestHeaders 请求头
     * @return 返回结果
     */
    public static String delete(String url, Map<String, String> requestHeaders) {
        log.info("###模拟http请求发起：url:{},header:{}", url, JsonUtil.obj2StringWithoutSensitiveField(requestHeaders));
        HttpDelete httpDelete = new HttpDelete(url);
        //添加请求头
        if (requestHeaders != null) {
            for (String key : requestHeaders.keySet()) {
                httpDelete.addHeader(key, requestHeaders.get(key));
            }
        }
        //执行
        return getString(httpDelete);
    }

    /**
     * @param httpRequestBase
     * @return
     */
    private static String getString(HttpRequestBase httpRequestBase) {
        long start = System.currentTimeMillis();

        CloseableHttpClient httpClient = closeableHttpClient;
        CloseableHttpResponse httpResponse = null;
        int statusCode = 200;
        String result = "";
        URI uri = null;
        try {
            uri = httpRequestBase.getURI();
            httpResponse = httpClient.execute(httpRequestBase);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            result = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            long end = System.currentTimeMillis();
            long cost = (end - start);
            parseStatusCode(statusCode, result, cost);
        } catch (ConnectTimeoutException e) {
            log.error("###请求链接超时, 状态码：{}，url：{}", statusCode, uri, e);
            throw new RuntimeException("系统繁忙，请稍后再尝试一下吧！");
        } catch (SocketTimeoutException e) {
            log.error("###请求读取超时, 状态码：{}，url：{}", statusCode, uri, e);
            throw new RuntimeException("系统繁忙，请稍后再尝试一下吧！");
        } catch (IOException | ParseException e) {
            log.error("", e);
        } finally {
            close(null, httpResponse);
        }
        long end = System.currentTimeMillis();
        long cost = (end - start);
        if (StringUtils.isNotBlank(result)) {
            String resLog = result.length() < 4096 ? result : result.substring(0, 4096);
            // resLog = CommonConstant.replaceAllSensitiveInfo(resLog);
            log.info("###模拟http请求结束，状态码：{}，耗时{}毫秒，响应： {}", statusCode, cost, resLog);
        } else {
            log.info("###模拟http请求结束，状态码：{}，耗时{}毫秒，响应： 无", statusCode, cost);
        }
        return result;
    }

    private static void parseStatusCode(int statusCode, String result, long cost) {
        if (statusCode == 503) {
            log.error("###模拟http请求结束，服务不可用，状态码：{}，耗时{}毫秒，响应： {}", statusCode, cost, result);
            throw new RuntimeException("系统功能暂时不可用，请稍后再尝试一下吧");
        } else if (statusCode == 504) {
            log.error("###模拟http请求结束，请求超时，状态码：{}，耗时{}毫秒，响应： {}", statusCode, cost, result);
            throw new RuntimeException("504");
        } else if (statusCode == 401) {
            log.error("###模拟http请求结束，认证失败，状态码：{}，耗时{}毫秒，响应： {}", statusCode, cost, result);
            throw new RuntimeException("系统内部错误，请联系工程师处理");
        } else if (statusCode != 200) {
            log.error("###模拟http请求结束，第三方系统内部错误 状态码：{}，耗时{}毫秒，响应： {}", statusCode, cost, result);
            throw new RuntimeException("系统内部错误，请联系工程师处理");
        }
    }

    /**
     * 关闭 CloseableHttpClient CloseableHttpResponse
     *
     * @param httpClient
     * @param httpResponse
     */
    private static void close(CloseableHttpClient httpClient, CloseableHttpResponse httpResponse) {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            if (httpResponse != null) {
                httpResponse.close();
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    /**
     * 对Map内所有value作utf8编码，拼接返回结果
     *
     * @param data
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String toQueryString(Map<?, ?> data) {
        try {
            StringBuffer queryString = new StringBuffer();
            for (Map.Entry<?, ?> pair : data.entrySet()) {
                queryString.append(pair.getKey() + "=");
                queryString.append(URLEncoder.encode((String) pair.getValue(),
                        "UTF-8") + "&");
            }
            if (queryString.length() > 0) {
                queryString.deleteCharAt(queryString.length() - 1);
            }
            return queryString.toString();
        } catch (UnsupportedEncodingException e) {
            log.error("转码失败 - data: {}", data, e);
            return null;
        }
    }

    private static String appendParam(String url, Map<String, Object> params) {
        if (params != null) {
            String param = "";
            for (String key : params.keySet()) {
                if (!Objects.isNull(params.get(key))) {
                    param = param + key + "=" + params.get(key) + "&";
                } else {
                    param = param + key + "=" + "" + "&";
                }

            }
            url = url + "?" + param;
        }
        return url;
    }
}
