package com.mrhi2024.ex67retrofitimageupload

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.loader.content.CursorLoader
import com.bumptech.glide.Glide
import com.mrhi2024.ex67retrofitimageupload.databinding.ActivityMainBinding
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri: Uri? = it.data?.data
            if (uri != null) {
                Glide.with(this).load(uri).into(binding.iv)


                //Retrofit 을 이용하여 Http 통신을 할때
                //파일을 업로드 하려면 파일의 실제 경로가 필여함
                //uri 콘텐츠 경로임 - 그래서 uri에 해당하는 파일의 실제경로를 구해와야 함
                // Uri --> 절대주소(실제 물리적인 저장소 위치) 변환
//            AlertDialog.Builder(this).setMessage(uri.toString()).create().show() 경로르 알아보기

                imgPath = getRealPathFromUri(uri)

            }


        }

    //Uri를 전달받아 실제 파일 경로를 리턴해주는 기능 메소드 구현하기
    private fun getRealPathFromUri(uri: Uri): String? {

        //android 10 버전 부터는 Uri를 통해 파일의 실제 경로를 얻을 수 있는 방법이 없어졌음
        //그래서 uri에 해당하는 파일을 복사하여 임시로 파일을 만들고 그 파일의 경로를 이용하여 서버에 전송

        // Uri[ 미디어저장소의 DB주소 ] 로 부터 파일의 이름을 얻어오기 - DB SELECT 쿼리작업을 해주는 기능을 가진 객체를 이용
        val cursorLoader: CursorLoader = CursorLoader(this, uri, null, null, null, null)
        val cursor: Cursor? = cursorLoader.loadInBackground()
        val fileName:String? = cursor?.run {
            moveToFirst()
            getString(getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
        }//-------------------------------------------------------------------------------------


        //복사본이 저장될 파일의 경로와 파일명 확장자
        val file: File = File(externalCacheDir, fileName.toString())


        // 복사작업 수행
        val inputStream:InputStream = contentResolver.openInputStream(uri) ?: return null
        val outputStream:OutputStream = FileOutputStream(file)

        // 파일복사
        while (true){
            val buf:ByteArray = ByteArray(1024) // 빈 바이트 배열 [ 길이:1KB ]
            val len:Int = inputStream.read(buf) // 스트림을 통해 읽어들인 바이트들을 buf 배열에 넣어줌 -- 읽어들인 바이트 수를 리턴해 줌
            if (len<= 0) break


            outputStream.write(buf, 0,len)
        }

        // 반복문이 끝났으면 복사가 완료된것임
        inputStream.close()
        outputStream.close()

        AlertDialog.Builder(this).setMessage(file.absolutePath).create().show()

        return file.absolutePath
    }

    // 선택한 이미지의 절대경로를 저장할 변수
    private var imgPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnSelect.setOnClickListener { clickSelect() }
        binding.btnUpload.setOnClickListener { clickUpload() }

    }

    private fun clickSelect() {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Intent(MediaStore.ACTION_PICK_IMAGES) else Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).setType("image/*")
        resultLauncher.launch(intent)
    }

    private fun clickUpload() {
        //이미지 선택이 없다면 업로드 안함
        //if(imgPath == null) return
        imgPath ?: return

        //레트로핏 라이브러리를 이용하여 이미지 업로드 5단계
        //1. retrofit 객체 생성
        val builder: Retrofit.Builder = Retrofit.Builder()
        builder.baseUrl("http://nameskdlxm.dothome.co.kr")
        builder.addConverterFactory(ScalarsConverterFactory.create())
        val retrofit: Retrofit = builder.build()

        //2. 레트로핏 서비스 명세서 인터페이스 설계하고 객체로 생성
        val retrofitService: RetrofitSevice = retrofit.create(RetrofitSevice::class.java)

        //3. 전송할 파일을 특별한 박스(MultiparBody.Part 객체)로 포장하기
        val file: File = File(imgPath)
        val requestBody: RequestBody = RequestBody.create(MediaType.parse("image/*"), file) //일종의 진공팩 [파일을 포장]
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("img1", file.name, requestBody) //식별자, 파일명, 요청Body [택배 스티로폼 상자]

        //4. 추상메소드를 호출
        val call: Call<String> = retrofitService.uploadImage(part)

        //5. 네트워크 작업 실행 - 비동기 방식으로
        call.enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                val s: String? = response.body()
                AlertDialog.Builder(this@MainActivity).setMessage(s).create().show()
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Toast.makeText(this@MainActivity, "오류 : ${t.message}", Toast.LENGTH_SHORT).show()
            }

        })
    }

}