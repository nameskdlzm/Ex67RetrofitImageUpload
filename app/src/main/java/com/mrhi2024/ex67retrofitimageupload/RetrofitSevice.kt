package com.mrhi2024.ex67retrofitimageupload

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

interface RetrofitSevice {

    // 원하는 작업 명세 파일 1개를 서버에 전송해주는 코드를 만들어줘
    @Multipart
    @POST("/05Retrofit/aaa.php")
    fun uploadImage(@Part file: MultipartBody.Part) : Call<String>


}