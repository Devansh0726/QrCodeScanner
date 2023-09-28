package com.version1chat.kotlinqrcodescanner

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

import com.version1chat.kotlinqrcodescanner.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101

        private const val TAG = "MAIN_TAG"
    }

    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>

    private var imageUri:Uri? = null

    private var barCodeScannerOptions: BarcodeScannerOptions? = null
    private var barCodeScanner: BarcodeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        cameraPermission = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

//        The following formats are supported:
//
//        Code 128 (FORMAT_CODE_128),   Code 39 (FORMAT_CODE_39), Code 93 (FORMAT_CODE_93),   Codabar (FORMAT_CODABAR)
//        EAN-13 (FORMAT_EAN_13),   EAN-8 (FORMAT_EAN_8), ITF (FORMAT_ITF),   UPC-A (FORMAT_UPC_A),
//        UPC-E (FORMAT_UPC_E),   QR Code (FORMAT_QR_CODE), PDF417 (FORMAT_PDF417), Aztec (FORMAT_AZTEC),
//        Data Matrix (FORMAT_DATA_MATRIX)
        barCodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()

        barCodeScanner = BarcodeScanning.getClient(barCodeScannerOptions!!)



        binding?.btnCamera?.setOnClickListener {
           if (checkCameraPermission()){
               pickImageCamera()
           } else{
               requestCameraPermission()
           }
        }

        binding?.btnGallery?.setOnClickListener {
            if (checkStoragePermission()){
                pickImageGallery()
            } else{
                requestStoragePermission()
            }
        }

        binding?.btnScan?.setOnClickListener {

            if (imageUri == null){
                showToast("Pick Image First")
            } else{
                detectResultFromImage()
            }
        } 
    }

    private fun detectResultFromImage(){
        Log.d(TAG, "detectResultFromImage:")
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)

            val barCodeResult = barCodeScanner!!.process(inputImage)
                .addOnSuccessListener {barcodes ->
                   extractBarcodeQrCodeInfo(barcodes)
            }
                .addOnFailureListener{ e ->
                   Log.e(TAG, "detectResultFromImage: ", e)
                    showToast("Failed scanning due to ${e.message}")
                }

        } catch (e:Exception){
            Log.e(TAG, "detectResultFromImage: ", e)
            showToast("Failed scanning due to ${e.message}")
        }
    }

    private fun extractBarcodeQrCodeInfo(barcodes: List<Barcode>) {
//      Get information from barcodes
        for (barcode in barcodes){
            val bound = barcode.boundingBox
            val corners = barcode.cornerPoints

//            raw info scanned from QR code/ barcode
            val rawValue = barcode.rawValue
            Log.d(TAG, "extractBarcodeQrCodeInfo: rawValue: ${rawValue}")

            val valueType = barcode.valueType
            when(valueType){
                Barcode.TYPE_WIFI ->{
//                To get wifi related data
                    val typeWifi = barcode.wifi

                    val ssid = "${typeWifi?.ssid}"
                    val password = "${typeWifi?.password}"
                    var encryptionType = "${typeWifi?.encryptionType}"

                    if (encryptionType == "1"){
                        encryptionType = "OPEN"
                    } else if (encryptionType == "2"){
                        encryptionType = "WPA"
                    } else if (encryptionType == "3"){
                        encryptionType = "WEP"
                    }
                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_WIFI")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: ssid: $ssid")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: password: $password")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: encryptionType: $encryptionType")

                    binding?.tvResult?.text = "TYPE_WIFI \nssid: $ssid \npassword: $password " +
                            "\nencryptionType: $encryptionType \n\nrawValue: $rawValue"
                }
                Barcode.TYPE_URL ->{
//                 TO get url related data
                    val typeUrl = barcode.url
//                 get all info about the url
                    val title = "${typeUrl?.title}"
                    val url = "${typeUrl?.url}"

                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_URL")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: title: $title")
                    Log.d(TAG, "extractBarcodeQrcodeInfo: url: $url")

                    binding?.tvResult?.text = "TYPE_URL \ntitle: $title \nurl: $url \n\nrawValue: $rawValue"

                }
                Barcode.TYPE_EMAIL ->{

                    val typeEmail = barcode.email

                    val address = "${typeEmail?.address}"
                    val body = "${typeEmail?.body}"
                    val subject = "${typeEmail?.subject}"

                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_EMAIL")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: address: $address")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: body: $body")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: subject: $subject")

                    binding?.tvResult?.text = "TYPE_EMAIL \naddress: $address \nbody: $body \nsubject: $subject \n\nrawValue: $rawValue"
                }
                Barcode.TYPE_CONTACT_INFO ->{

                    val typeContact = barcode.contactInfo

                    val title = "${typeContact?.title}"
                    val organization = "${typeContact?.organization}"
                    val name = "${typeContact?.name?.first} ${typeContact?.name?.last}"
                    val phone = "${typeContact?.name?.first} ${typeContact?.phones?.get(0)?.number}"

                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_CONTACT_INFO")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: title: $title")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: organization: $organization")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: name: $name")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: phone: $phone")

                    binding?.tvResult?.text = "TYPE_CONTACT_INFO \ntitle: $title \norganization: $organization \nname: $name \nphone: $phone \n\nrawValue: $rawValue"
                } else ->{
                    binding?.tvResult?.text = "rawValue: $rawValue "
                }
            }



        }
    }

    private fun pickImageGallery(){
//        intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK)
//        set type of file we want to pick i.e image
        intent.type = "image/*"
        galleryActionResultLauncher.launch(intent)
    }

    private val galleryActionResultLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()){ result ->
//        here we will receive image if picked from gallery
        if (result.resultCode == Activity.RESULT_OK){
//         image picked, get the uri of the image picked
            val data = result.data
//          save uri of image in imageUri
            imageUri = data?.data
            Log.d(TAG, "galleryActionResultLauncher: imageUri: $imageUri")
//          set to imageView
            binding?.ivImage?.setImageURI(imageUri)
        } else{
               showToast("Cancelled")
        }
    }

    private fun pickImageCamera(){


//
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Image Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)


        cameraActionResultLauncher.launch(intent)

    }

    private val cameraActionResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){result ->

        if (result.resultCode == Activity.RESULT_OK){

            val data = result.data

            Log.d(TAG, "cameraActionResultLauncher: imageUri: $imageUri")

            binding?.ivImage?.setImageURI(imageUri)
        }
    }

// check if storage permission is allowed or not return true if allowed, false if not allowed
    private fun checkStoragePermission(): Boolean{

        val result = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED)

        return result
    }

    private fun requestStoragePermission(){

        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun checkCameraPermission(): Boolean{
        val resultCamera = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)) == PackageManager.PERMISSION_GRANTED
        val resultStorage = (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED

        return resultCamera && resultStorage
    }

    private fun requestCameraPermission(){

        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CAMERA_REQUEST_CODE ->{
                  if (grantResults.isNotEmpty()){

                      val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                      val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                      if (cameraAccepted && storageAccepted){
                          pickImageCamera()
                      } else{
                          showToast("Camera & Storage Permission are required")
                      }
                  }
            }


            STORAGE_REQUEST_CODE ->{

                if (grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (storageAccepted){
                        pickImageGallery()
                    } else{
                        showToast("Storage permission is required...")
                    }
                }
            }

        }
    }


    private fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}