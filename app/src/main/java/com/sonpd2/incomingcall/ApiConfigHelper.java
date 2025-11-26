package com.sonpd2.incomingcall;

import android.content.Context;
import android.content.SharedPreferences;

public class ApiConfigHelper {
    private static final String PREFS_NAME = "api_config";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_API_USER = "api_user";
    private static final String KEY_COOKIE = "cookie";

    // Default values
    private static final String DEFAULT_API_URL = "https://vops.viettel.vn/api/v1/contacts.search-by-org";
    private static final String DEFAULT_API_KEY = "Q8ZEdZq7sKOAjxLPeprHOt4Xj6YHuVVRRrFLJoklOGk";
    private static final String DEFAULT_API_USER = "jE5TLGK33qi7GR3nW";
    private static final String DEFAULT_COOKIE = "_ga_MD06Q9YWMN=GS2.1.s1759849121^$o1^$g1^$t1759849142^$j39^$l0^$h0; _ga=GA1.2.1787459265.1759849122; _gcl_au=1.1.707934688.1760350752; _ga_VH8261689Q=GS2.1.s1760356970^$o2^$g0^$t1760356970^$j60^$l0^$h0; _ga_Z30HDXVFSV=GS2.1.s1760356970^$o2^$g0^$t1760356970^$j60^$l0^$h0; _fbp=fb.1.1760350753572.544475374238167048; ticket=eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCIsImtpZCI6ImFmMzU2OGU2LTgxNGMtNGY3MS1iNDQ5LTMxODlmNDgyMWY4OCJ9.ZXlKaGJHY2lPaUprYVhJaUxDSmxibU1pT2lKQk1USTRRMEpETFVoVE1qVTJJaXdpWTNSNUlqb2lTbGRVSWl3aWRIbHdJam9pU2xkVUlpd2lhMmxrSWpvaU1XRmtaRE14Wm1VdE1EZzBNeTAwWlRnNUxUZzVaREl0T1dJNU1UTmxNamMwTXpVNUluMC4uQ0J5bDZEekV4S3VBbGtCUzVsSWFQQS5kLXVaN2ZtRVJReEJ6RnQtTG5mZlJNOXRkbVo4U2NjcEF2TEw4ZTF3QWpRcXZ4b0ctVmZoZDZ1OTRabzJRQ2VreUtfUGZZRERrYjF3bVhubl9jcFlBeEV4MFhUUExUTl9UX0lxWVNSQTJuNmw0NnVFbmo5ZEJTbEtZZmhsMFBFRHNtZjcyT29DTjFiSmhhLUYwZVI3R3dRMXlYOEc2TmN6VHQweHdVczZrVjVaSmNadm9ILXl4V19QZkY2Tk5zbGxmcWVaMENDUXJ0ckFHNzRaVG9Wb1hpOWlTbEU0UWdab0E0dl9qVWtBYW5neFVhYmxKNU0td0F5OXNKaFZWT3pScFh5TlZTUDRJckc0di0tN0hhcWNIdnZCSk5iTnV4ZmxkME5Td0h2SVZudGJUMWw0UEpaTlZycVdHY2tfdHM2RlU4blVEaG02SVpVQWVubnNDUGdnWjRmS3NBMk9sNUhrQ1hJdTR2VU1hSnZZUVpHR1lGRUM0YTYxci1UamNrVWtBQVhtS0wzY0dVMjdUdFRHaVBmU1NzWG1kcGQxcFhPd3FNSFl6SnM5U01mMWJLc0xMR2p3blg2bFVrbmtLTEx3VWZGbWxmdzBMZk5UanptZFBYdk9NMmEwd1poRmUwUEJkVU1KdEVUR1duSEliZ0tvWTgta1k2ZFlZVFNINldoQmpMR1lKNFJLNkVDZ2FqQmhOTDFmOVN2dk5XeGE2TnlvRi1NaW1SNGtmdFNYcUwxbjJhYVRZLTQ0dWFmR0pidFpfbm5TSjNIaXpYMV80bnVPQmJjbWtCM1c0SXZIT0ZCSGw2blRtQWd2RTJVaWNKMUhWOUZmaUxGbU14QVFGZ3ppQlNvMWpzOGlfZVRsMWlkT001ckNHbTFNTEJYdGZTMlNGcFhwN21oVHZyenNrLXFvN0hqd2hoSFhPYmh2NVVERTFnakdjN2hRNFUzbklGblNobnlyUG5NYmk5R1Nma1FhRjliWFRqUkoyWVdxQi1ZU1hDSG1LTi1Wc2JHaW0xY3VnWThiUVlxMUh1TUhLTmJycjlhT0FxajlNSXA3OU93U05WVEFQNFlQTDRSSGpMclllY1RYYkpoblN6X3VPaFdNNFh0LVhfUDdodThNNS05NTExUGZ5UFRTRUlfbFh1dWpmazhMbzc3OWw5akJHSUkwYm9ScWg4UkJPTHZjMWhWaDFpNm5lb3FkMXNKekMtWXlONUMwRUVxY3pKU1FtY25GeUFoWjlWVHJCOERQSTlUazdQVFhtT0MzNmFzNGdYUWI4MEtfMFNYY1lnZHJmeV92Q3R4azZDU0xMWDR1Mk55V1JRVDdEU2Q4VjZJZWpRTllwT2RZVmE1bWhEdWU3V0h0bXpjelpuSU5jVFdBWHNKQklQXzdWTENOc09EUzhRaWd5ckpYN2pIeF9MbkFpYkM2emp0RzJOcU1DcGcwZ2RDNDExQUhEalJzc2RUVkltY0pKOXlBUkdlaURjWmhjaGpFdjV0MTdocnlCSlhxZ1ItUDZ0c3FLbnY2MEFSWkZ4dC1JajBwRkpQRDZ2OHFtaU9wTngzNjhwUm0zVm40UHRaamNtejRYejlYT3l4MzIzbmt1NGlteF83QnBZTTE2bzR1VkpIV0FzblVPWlJMazZHSHk0Z1ZjaFRnOWMzaG5qMmJnU1psNGxDQWpYOUZQaU44RUR1Q3VpZjdreU11LUVqNzZiZm5mWm96MklFQ3cwa2piZlFZU2FVUldHRFB2b2FVUHhnaXREcnlnVVpjdFRYSHRTbFlpaXhVOHB0TnFldmJaM0hHRS1XcHFseGc2WklNdVZ4cFVlRnJWLVF1Z2pvVTByYWVRd3psOHMwRWMyNl9wOENjLWFQLWdudlNJaXoxNFlOX1pieGFhUWhDdVZPdnJobWI4STJ6LWJtMTVYb1NxaEdnRXNfeFRHUS5Yd0tzQURhVmFaSW9lcG96UTVsTWV3.psQA8gbmnAIh3AMafdsVaCZr2eCvFMm-6K6DxqnRb0rweoXSqmex6GGPYeH2X5d8-9-M2F7RXLmVig4GtxpMew; rc_uid=jE5TLGK33qi7GR3nW; rc_token=830MupdSqtZ3YTMTssXnI5nPobdvNscG5-c7fo85jT9";

    private SharedPreferences prefs;

    public ApiConfigHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getApiUrl() {
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL);
    }

    public void setApiUrl(String apiUrl) {
        prefs.edit().putString(KEY_API_URL, apiUrl).apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, DEFAULT_API_KEY);
    }

    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getApiUser() {
        return prefs.getString(KEY_API_USER, DEFAULT_API_USER);
    }

    public void setApiUser(String apiUser) {
        prefs.edit().putString(KEY_API_USER, apiUser).apply();
    }

    public String getCookie() {
        return prefs.getString(KEY_COOKIE, DEFAULT_COOKIE);
    }

    public void setCookie(String cookie) {
        prefs.edit().putString(KEY_COOKIE, cookie).apply();
    }

    public void resetToDefaults() {
        prefs.edit()
                .putString(KEY_API_URL, DEFAULT_API_URL)
                .putString(KEY_API_KEY, DEFAULT_API_KEY)
                .putString(KEY_API_USER, DEFAULT_API_USER)
                .putString(KEY_COOKIE, DEFAULT_COOKIE)
                .apply();
    }
}

