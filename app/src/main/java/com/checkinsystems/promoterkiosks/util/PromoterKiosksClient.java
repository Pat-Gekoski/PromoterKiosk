package com.checkinsystems.promoterkiosks.util;


import com.checkinsystems.promoterkiosks.model.ConfigurationObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PromoterKiosksClient {

    @POST("login/webhook.php")
    Call<ConfigurationObject> sendStringToServer(@Body ConfigurationObject configObj);

}
